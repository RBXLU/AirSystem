package com.rbxlu.airsystem.client.handler;

import com.rbxlu.airsystem.registry.ModSounds;
import com.rbxlu.airsystem.util.AirSystemConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ImpactVisuals {
    private static final double SHOCK_SPEED = 2.4D;

    private static final int SHOCK_TICKS = 18;

    private static final int COLUMN_TICKS = 150;

    private record DelayedSound(Vec3 at, SoundEvent sound, float volume, float pitch, int delay) {
    }

    private static final class Blast {
        private final Vec3 at;
        private final float power;
        private final boolean incendiary;
        private final boolean wreck;
        private final float detail;
        private int age;

        private Blast(Vec3 at, float power, boolean incendiary, boolean wreck, float detail) {
            this.at = at;
            this.power = power;
            this.incendiary = incendiary;
            this.wreck = wreck;
            this.detail = detail;
        }
    }

    private static final List<DelayedSound> PENDING = new ArrayList<>();
    private static final List<Blast> BLASTS = new ArrayList<>();

    public static void spawn(Vec3 at, float power, boolean incendiary, boolean wreck) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }

        double distance = minecraft.player.position().distanceTo(at);
        ScreenShakeHandler.add(at, power * (wreck ? 0.5F : 1.0F));
        queueSounds(at, power, distance);

        float detail = (float) Mth.clamp(1.35D - distance / 130.0D, 0.0D, 1.0D);
        if (detail <= 0.0F) {
            return;
        }

        flash(minecraft, at, power, distance, wreck);
        fireball(level, at, power, incendiary, detail);
        BLASTS.add(new Blast(at, power, incendiary, wreck, detail));
    }

    private static void flash(Minecraft minecraft, Vec3 at, float power, double distance, boolean wreck) {
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        Vec3 eye = minecraft.player.getEyePosition();
        boolean visible = minecraft.level.clip(new ClipContext(eye, at,
                ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, minecraft.player))
                .getType() == HitResult.Type.MISS;

        float value = (float) (power / (distance * 0.14D + 2.2D));
        if (!visible) {
            value *= 0.22F;
        }
        if (wreck) {
            value *= 0.5F;
        }
        BlastOverlay.flash(Mth.clamp(value, 0.0F, 1.0F));
    }

    private static void fireball(ClientLevel level, Vec3 at, float power, boolean incendiary, float detail) {
        level.addParticle(ParticleTypes.FLASH, at.x, at.y + 0.5D, at.z, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y + 0.4D, at.z, 0.0D, 0.0D, 0.0D);

        int count = (int) (power * 16.0F * detail);
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double elevation = level.random.nextDouble() * Math.PI * 0.6D - 0.15D;
            double speed = 0.2D + level.random.nextDouble() * power * 0.2D;
            double vx = Math.cos(angle) * Math.cos(elevation) * speed;
            double vy = Math.sin(elevation) * speed;
            double vz = Math.sin(angle) * Math.cos(elevation) * speed;

            level.addParticle(ParticleTypes.FLAME, at.x, at.y + 0.4D, at.z, vx, vy, vz);
            if (i % 2 == 0) {
                level.addParticle(ParticleTypes.SMALL_FLAME, at.x, at.y + 0.4D, at.z,
                        vx * 1.4D, vy * 1.4D, vz * 1.4D);
            }
            if (i % 3 == 0) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.5D, at.z,
                        vx * 0.5D, vy * 0.5D, vz * 0.5D);
            }
            if (incendiary && i % 4 == 0) {
                level.addParticle(ParticleTypes.LAVA, at.x, at.y + 0.3D, at.z, vx, vy, vz);
            }
        }

        int sparks = (int) (power * 5.0F * detail);
        for (int i = 0; i < sparks; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double elevation = level.random.nextDouble() * Math.PI * 0.55D;
            double speed = 0.6D + level.random.nextDouble() * power * 0.35D;
            level.addParticle(ParticleTypes.FIREWORK, at.x, at.y + 0.5D, at.z,
                    Math.cos(angle) * Math.cos(elevation) * speed,
                    Math.sin(elevation) * speed,
                    Math.sin(angle) * Math.cos(elevation) * speed);
        }

        BlockState ground = level.getBlockState(BlockPos.containing(at).below());
        if (!ground.isAir()) {
            int debris = (int) (power * 10.0F * detail);
            for (int i = 0; i < debris; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2.0D;
                double speed = 0.25D + level.random.nextDouble() * power * 0.18D;
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                        at.x, at.y + 0.2D, at.z,
                        Math.cos(angle) * speed,
                        0.35D + level.random.nextDouble() * 0.5D,
                        Math.sin(angle) * speed);
            }
        }
    }

    private static void queueSounds(Vec3 at, float power, double distance) {
        int delay = (int) (distance / AirSystemConfig.SOUND_BLOCKS_PER_TICK);
        if (distance < 48.0D) {
            PENDING.add(new DelayedSound(at, ModSounds.EXPLOSION_NEAR.get(),
                    Math.min(1.0F + power * 0.3F, 4.0F), 0.9F + (float) Math.random() * 0.2F, delay));
            PENDING.add(new DelayedSound(at, ModSounds.DEBRIS_FALL.get(), 1.4F, 1.0F, delay + 14));
        } else {
            float volume = (float) Mth.clamp(6.0D - distance / 60.0D, 1.0D, 6.0D);
            PENDING.add(new DelayedSound(at, ModSounds.EXPLOSION_DISTANT.get(), volume, 0.8F, delay));
        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }

        tickSounds(level);

        Iterator<Blast> iterator = BLASTS.iterator();
        while (iterator.hasNext()) {
            Blast blast = iterator.next();
            blast.age++;
            if (!advance(level, blast)) {
                iterator.remove();
            }
        }
    }

    private static boolean advance(ClientLevel level, Blast blast) {
        if (blast.age <= 5) {
            expandingFire(level, blast);
        }
        if (blast.age <= SHOCK_TICKS) {
            groundWave(level, blast);
            airRing(level, blast);
        }
        if (blast.age <= COLUMN_TICKS) {
            smokeColumn(level, blast);
            return true;
        }
        return false;
    }

    private static void expandingFire(ClientLevel level, Blast blast) {
        int count = (int) (blast.power * 4.0F * blast.detail);
        double radius = blast.power * 0.35D * blast.age;
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double elevation = level.random.nextDouble() * Math.PI - Math.PI / 2.0D;
            double x = blast.at.x + Math.cos(angle) * Math.cos(elevation) * radius;
            double y = blast.at.y + 0.5D + Math.sin(elevation) * radius * 0.7D;
            double z = blast.at.z + Math.sin(angle) * Math.cos(elevation) * radius;
            level.addParticle(blast.age < 3 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE,
                    x, y, z, 0.0D, 0.02D, 0.0D);
        }
    }

    private static void groundWave(ClientLevel level, Blast blast) {
        double radius = SHOCK_SPEED * blast.age * Math.min(1.0D, blast.power / 4.0D + 0.4D);
        int points = (int) Mth.clamp(radius * 3.0D * blast.detail, 6.0D, 90.0D);
        double fade = 1.0D - blast.age / (double) SHOCK_TICKS;

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points + level.random.nextDouble() * 0.1D;
            double x = blast.at.x + Math.cos(angle) * radius;
            double z = blast.at.z + Math.sin(angle) * radius;
            double y = blast.at.y + 0.1D;

            level.addParticle(ParticleTypes.DUST_PLUME, x, y, z,
                    Math.cos(angle) * 0.12D, 0.04D * fade, Math.sin(angle) * 0.12D);
            if (i % 3 == 0) {
                level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z,
                        Math.cos(angle) * 0.2D, 0.02D, Math.sin(angle) * 0.2D);
            }
        }

        if (blast.age <= 4 && blast.detail > 0.5F) {
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2.0D * i) / 6.0D;
                level.addParticle(ParticleTypes.GUST,
                        blast.at.x + Math.cos(angle) * radius * 0.6D, blast.at.y + 0.4D,
                        blast.at.z + Math.sin(angle) * radius * 0.6D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static void airRing(ClientLevel level, Blast blast) {
        if (blast.age > 10 || blast.detail < 0.4F) {
            return;
        }
        double radius = SHOCK_SPEED * 1.4D * blast.age;
        int points = (int) Mth.clamp(radius * 2.0D * blast.detail, 8.0D, 64.0D);
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            double elevation = (level.random.nextDouble() - 0.3D) * 0.9D;
            level.addParticle(ParticleTypes.CLOUD,
                    blast.at.x + Math.cos(angle) * Math.cos(elevation) * radius,
                    blast.at.y + 0.8D + Math.sin(elevation) * radius * 0.5D,
                    blast.at.z + Math.sin(angle) * Math.cos(elevation) * radius,
                    0.0D, 0.01D, 0.0D);
        }
    }

    private static void smokeColumn(ClientLevel level, Blast blast) {
        int puffs = Math.max(1, (int) (blast.power * 0.9F * blast.detail));
        double rise = blast.age * 0.07D;
        double spread = blast.power * (0.22D + blast.age * 0.006D);

        for (int i = 0; i < puffs; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double offset = level.random.nextDouble() * spread;
            level.addParticle(ParticleTypes.LARGE_SMOKE,
                    blast.at.x + Math.cos(angle) * offset,
                    blast.at.y + 0.6D + rise,
                    blast.at.z + Math.sin(angle) * offset,
                    Math.cos(angle) * 0.01D,
                    0.06D + level.random.nextDouble() * 0.05D,
                    Math.sin(angle) * 0.01D);
        }

        if (blast.incendiary && blast.age % 3 == 0) {
            level.addParticle(ParticleTypes.ASH,
                    blast.at.x + (level.random.nextDouble() - 0.5D) * spread * 2.0D,
                    blast.at.y + 1.0D + rise,
                    blast.at.z + (level.random.nextDouble() - 0.5D) * spread * 2.0D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    private static void tickSounds(ClientLevel level) {
        if (PENDING.isEmpty()) {
            return;
        }
        List<DelayedSound> ready = new ArrayList<>();
        PENDING.removeIf(sound -> {
            if (sound.delay() <= 0) {
                ready.add(sound);
                return true;
            }
            return false;
        });
        for (DelayedSound sound : ready) {
            level.playLocalSound(sound.at().x, sound.at().y, sound.at().z, sound.sound(),
                    SoundSource.BLOCKS, sound.volume(), sound.pitch(), false);
        }
        PENDING.replaceAll(sound -> new DelayedSound(sound.at(), sound.sound(), sound.volume(),
                sound.pitch(), sound.delay() - 1));
    }

    public static void clear() {
        PENDING.clear();
        BLASTS.clear();
    }

    private ImpactVisuals() {
    }
}
