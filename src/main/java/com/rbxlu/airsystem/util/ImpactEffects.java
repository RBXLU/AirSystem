package com.rbxlu.airsystem.util;

import com.rbxlu.airsystem.network.payload.ImpactPayload;
import com.rbxlu.airsystem.registry.ModSounds;
import com.rbxlu.airsystem.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class ImpactEffects {
    private static final List<Shockwave> ACTIVE_WAVES = new ArrayList<>();

    private static final class Shockwave {
        private final ServerLevel level;
        private final Vec3 origin;
        private final int maxRadius;
        private final List<BlockPos> fragile;
        private int tick;
        private int cursor;

        private Shockwave(ServerLevel level, Vec3 origin, int maxRadius, List<BlockPos> fragile) {
            this.level = level;
            this.origin = origin;
            this.maxRadius = maxRadius;
            this.fragile = fragile;
        }

        private boolean tick() {
            tick++;
            double frontRadius = maxRadius * (tick / (double) AirSystemConfig.shockwaveTicks());
            boolean brokeAny = false;

            while (cursor < fragile.size()) {
                BlockPos pos = fragile.get(cursor);
                if (pos.distToCenterSqr(origin) > frontRadius * frontRadius) {
                    break;
                }
                cursor++;
                BlockState state = level.getBlockState(pos);
                if (!isFragile(state)) {
                    continue;
                }

                double distance = Math.sqrt(pos.distToCenterSqr(origin));
                double chance = 1.0D - (distance / (maxRadius + 1.0D)) * 0.85D;
                if (level.random.nextDouble() > chance) {
                    continue;
                }
                level.destroyBlock(pos, false);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        6, 0.25D, 0.25D, 0.25D, 0.05D);
                brokeAny = true;
            }

            if (brokeAny) {
                double soundRadius = Math.max(4.0D, frontRadius);
                Vec3 front = origin.add(
                        (level.random.nextDouble() - 0.5D) * soundRadius,
                        0.0D,
                        (level.random.nextDouble() - 0.5D) * soundRadius);
                level.playSound(null, front.x, front.y, front.z, ModSounds.WINDOW_SHATTER.get(),
                        SoundSource.BLOCKS, 2.0F, 0.9F + level.random.nextFloat() * 0.3F);
            }

            return cursor >= fragile.size() || tick > AirSystemConfig.shockwaveTicks() + 4;
        }
    }

    public static void detonateWarhead(ServerLevel level, Vec3 at, float power, boolean incendiary,
                                       int windowRadius, @Nullable Entity source) {
        // Damage and terrain only: the visuals and the report are drawn client-side,
        // delayed by the distance to each observer.
        //
        // Vanilla explosion power is radius-shaped, and a warhead measured against TNT
        // barely scratched stone. The multiplier is what a shaped charge earns over a
        // block of gunpowder.
        level.explode(source, level.damageSources().explosion(source, null), null,
                at.x, at.y, at.z, power * AirSystemConfig.blastPower(), incendiary,
                Level.ExplosionInteraction.TNT,
                false, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER,
                ModSounds.SILENCE);

        throwEntities(level, at, power);

        if (incendiary && AirSystemConfig.startFires()) {
            igniteArea(level, at, Math.max(3, (int) (power * 1.6F)));
        }

        if (windowRadius > 0 && AirSystemConfig.breakWindows()) {
            queueShockwave(level, at, windowRadius);
        }

        broadcastImpact(level, at, power, incendiary, false);
    }

    public static void crashWreck(ServerLevel level, Vec3 at, @Nullable Entity source) {
        level.explode(source, level.damageSources().explosion(source, null), null,
                at.x, at.y, at.z, 1.6F, false, Level.ExplosionInteraction.TNT,
                false, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER,
                ModSounds.SILENCE);
        queueShockwave(level, at, 6);
        broadcastImpact(level, at, 1.6F, false, true);
    }

    /**
     * The blast wave itself: everything inside it is thrown outwards and upwards,
     * hardest at the centre. Vanilla explosion knockback stops at the damage radius,
     * which for a warhead this size is far too small to read as a shockwave.
     */
    private static void throwEntities(ServerLevel level, Vec3 at, float power) {
        double radius = power * 3.0D;
        AABB box = new AABB(at.x - radius, at.y - radius, at.z - radius,
                at.x + radius, at.y + radius, at.z + radius);

        for (Entity entity : level.getEntities((Entity) null, box, Entity::isPushable)) {
            Vec3 away = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D).subtract(at);
            double distance = away.length();
            if (distance > radius || distance < 1.0E-4D) {
                continue;
            }
            double falloff = 1.0D - distance / radius;
            Vec3 push = away.scale(1.0D / distance).scale(falloff * falloff * power * 0.55D);
            entity.setDeltaMovement(entity.getDeltaMovement().add(push.x, push.y + falloff * 0.35D, push.z));
            entity.hurtMarked = true;
        }
    }

    private static void igniteArea(ServerLevel level, Vec3 at, int radius) {
        BlockPos center = BlockPos.containing(at);
        int attempts = radius * radius;
        for (int i = 0; i < attempts; i++) {
            BlockPos pos = center.offset(
                    level.random.nextInt(radius * 2 + 1) - radius,
                    level.random.nextInt(5) - 2,
                    level.random.nextInt(radius * 2 + 1) - radius);
            if (pos.distSqr(center) > (double) radius * radius) {
                continue;
            }
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    private static void queueShockwave(ServerLevel level, Vec3 at, int radius) {
        BlockPos center = BlockPos.containing(at);

        int verticalRadius = Math.min(radius, 16);
        List<BlockPos> fragile = new ArrayList<>();

        BlockPos.betweenClosedStream(
                center.offset(-radius, -verticalRadius, -radius),
                center.offset(radius, verticalRadius, radius)
        ).forEach(pos -> {
            if (pos.distToCenterSqr(at) > (double) radius * radius) {
                return;
            }
            if (isFragile(level.getBlockState(pos))) {
                fragile.add(pos.immutable());
            }
        });

        if (fragile.isEmpty()) {
            return;
        }
        fragile.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(at)));
        ACTIVE_WAVES.add(new Shockwave(level, at, radius, fragile));
    }

    public static boolean isFragile(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.is(ModTags.Blocks.SHOCKWAVE_FRAGILE)) {
            return true;
        }

        return state.getBlock() instanceof TransparentBlock
                || state.getBlock() instanceof StainedGlassPaneBlock
                || state.is(Blocks.GLASS_PANE)
                || state.is(Blocks.TINTED_GLASS);
    }

    private static void broadcastImpact(ServerLevel level, Vec3 at, float power, boolean incendiary, boolean wreck) {
        ImpactPayload payload = new ImpactPayload(at.x, at.y, at.z, power, incendiary, wreck);
        double radiusSqr = AirSystemConfig.distantSoundRadius() * AirSystemConfig.distantSoundRadius();
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceToSqr(at) <= radiusSqr) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static void hitSparks(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.15D, 0.15D, 0.15D, 0.05D);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 4, 0.1D, 0.1D, 0.1D, 0.01D);
    }

    public static void tickShockwaves() {
        if (ACTIVE_WAVES.isEmpty()) {
            return;
        }
        Iterator<Shockwave> iterator = ACTIVE_WAVES.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick()) {
                iterator.remove();
            }
        }
    }

    public static void clear() {
        ACTIVE_WAVES.clear();
    }

    private ImpactEffects() {
    }
}
