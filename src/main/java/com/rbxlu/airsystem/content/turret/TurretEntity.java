package com.rbxlu.airsystem.content.turret;

import com.rbxlu.airsystem.content.drone.DroneFlight;
import com.rbxlu.airsystem.content.drone.DroneFlightManager;
import com.rbxlu.airsystem.network.payload.TracerPayload;
import com.rbxlu.airsystem.registry.ModItems;
import com.rbxlu.airsystem.registry.ModSounds;
import com.rbxlu.airsystem.util.AirSystemConfig;
import com.rbxlu.airsystem.util.TaczCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class TurretEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_TURRET_YAW =
            SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BARREL_PITCH =
            SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_AMMO =
            SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_AUTO =
            SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FIRING =
            SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.FLOAT);

    private static final float MAX_HEALTH = 260.0F;
    private static final float MIN_PITCH = -12.0F;
    private static final float MAX_PITCH = 86.0F;

    private final TurretKind kind;

    private int fireCooldown;
    private int reloadTicks;
    private int barrelIndex;

    private float recoil;
    @Nullable
    private DroneFlight autoTarget;
    private int lockTicks;

    private int scanCooldown;

    public TurretEntity(EntityType<? extends TurretEntity> type, Level level, TurretKind kind) {
        super(type, level);
        this.kind = kind;
        this.blocksBuilding = true;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();

        if (kind.isAutoOnly() && !level().isClientSide) {
            this.entityData.set(DATA_AUTO, true);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_TURRET_YAW, 0.0F);
        builder.define(DATA_BARREL_PITCH, 0.0F);
        builder.define(DATA_AMMO, 0);
        builder.define(DATA_AUTO, false);
        builder.define(DATA_FIRING, false);
        builder.define(DATA_HEALTH, MAX_HEALTH);
    }

    public TurretKind getKind() {
        return kind;
    }

    public float getTurretYaw() {
        return this.entityData.get(DATA_TURRET_YAW);
    }

    public float getBarrelPitch() {
        return this.entityData.get(DATA_BARREL_PITCH);
    }

    public int getAmmo() {
        return this.entityData.get(DATA_AMMO);
    }

    public void setAmmo(int ammo) {
        this.entityData.set(DATA_AMMO, Mth.clamp(ammo, 0, kind.getMagazine()));
    }

    public boolean isAutoMode() {
        return this.entityData.get(DATA_AUTO);
    }

    public boolean isFiring() {
        return this.entityData.get(DATA_FIRING);
    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH);
    }

    public float getRecoil() {
        return recoil;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !kind.isAutoOnly() && getPassengers().isEmpty();
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger)) {
            return;
        }

        float yawRad = (float) Math.toRadians(getTurretYaw());
        double offsetX = -Math.sin(yawRad) * -0.6D;
        double offsetZ = Math.cos(yawRad) * -0.6D;
        callback.accept(passenger, getX() + offsetX, getY() + kind.getHeight() * 0.62D, getZ() + offsetZ);
    }

    @Override
    public void tick() {
        super.tick();

        if (!isNoGravity()) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.06D, 0.0D).multiply(0.7D, 0.98D, 0.7D));
            move(MoverType.SELF, getDeltaMovement());
            if (onGround()) {
                setDeltaMovement(Vec3.ZERO);
            }
        }

        if (level().isClientSide) {
            recoil = Math.max(0.0F, recoil - 0.18F);
            if (isFiring()) {
                recoil = Math.min(1.0F, recoil + 0.5F);
                spawnMuzzleParticles();
            }
            return;
        }

        if (fireCooldown > 0) {
            fireCooldown--;
        }
        if (reloadTicks > 0) {
            reloadTicks--;
            this.entityData.set(DATA_FIRING, false);
            return;
        }

        LivingEntity gunner = getControllingPassenger();
        if (gunner != null) {
            tickManualAiming(gunner);
        } else if (isAutoMode()) {
            tickAutoEngagement();
        } else {
            this.entityData.set(DATA_FIRING, false);
            autoTarget = null;
        }
    }

    // The radar sweeps the sector instead of walking every flight each tick.
    private static final int SCAN_INTERVAL = 8;

    private void tickManualAiming(LivingEntity gunner) {
        float desiredYaw = gunner.getYRot();
        float desiredPitch = Mth.clamp(-gunner.getXRot(), MIN_PITCH, MAX_PITCH);
        traverseTowards(desiredYaw, desiredPitch);

        if (isFiring() && fireCooldown <= 0) {
            fireBurst(gunner);
        }
    }

    private void tickAutoEngagement() {
        boolean lost = autoTarget == null || !autoTarget.isAlive()
                || autoTarget.position().distanceTo(position()) > kind.getRange()
                || !autoTarget.state().isAirborne() || isFriendly(autoTarget);
        if (lost) {
            autoTarget = null;

            if (--scanCooldown <= 0) {
                scanCooldown = SCAN_INTERVAL;
                autoTarget = findTarget();
                lockTicks = 0;
                if (autoTarget != null) {
                    level().playSound(null, getX(), getY(), getZ(), ModSounds.RADAR_LOCK.get(),
                            SoundSource.NEUTRAL, 2.0F, 1.0F);
                }
            }
        }

        if (autoTarget == null) {
            this.entityData.set(DATA_FIRING, false);

            traverseTowards(getTurretYaw() + 0.6F, 12.0F);
            return;
        }

        Vec3 aim = leadTarget(autoTarget);
        Vec3 delta = aim.subtract(muzzlePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float desiredYaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0D / Math.PI)) - 90.0F;
        float desiredPitch = (float) (Mth.atan2(delta.y, horizontal) * (180.0D / Math.PI));
        traverseTowards(desiredYaw, Mth.clamp(desiredPitch, MIN_PITCH, MAX_PITCH));

        lockTicks++;
        boolean onTarget = Math.abs(Mth.degreesDifference(getTurretYaw(), desiredYaw)) < 4.0F
                && Math.abs(getBarrelPitch() - desiredPitch) < 4.0F;
        if (onTarget && lockTicks > 10 && fireCooldown <= 0) {
            fireBurst(null);
        } else {
            this.entityData.set(DATA_FIRING, false);
        }
    }

    private Vec3 leadTarget(DroneFlight target) {
        double distance = target.position().distanceTo(muzzlePosition());
        double shellSpeed = 12.0D;
        double flightTicks = distance / shellSpeed;
        return target.position().add(target.velocity().scale(flightTicks));
    }

    @Nullable
    private DroneFlight findTarget() {
        if (!(level() instanceof ServerLevel server)) {
            return null;
        }
        return DroneFlightManager.get(server).nearestHostile(position(), kind.getRange(),
                AirSystemConfig.friendlyRadius());
    }

    private boolean isFriendly(DroneFlight flight) {
        return DroneFlightManager.isFriendly(flight, position(), AirSystemConfig.friendlyRadius());
    }

    private float hitChance(DroneFlight target) {
        double distance = target.position().distanceTo(muzzlePosition());
        double proximity = Mth.clamp(1.0D - distance / Math.max(1.0F, kind.getRange()), 0.0D, 1.0D);
        double evasion = Mth.clamp(1.0D - target.velocity().length() / 4.0D, 0.3D, 1.0D);
        double base = kind.getAccuracy() * AirSystemConfig.autoHitChance();
        return (float) Mth.clamp(base * (0.4D + 0.6D * proximity) * evasion, 0.05D, 0.9D);
    }

    private void traverseTowards(float desiredYaw, float desiredPitch) {
        float traverseSpeed = 6.0F;
        float yaw = getTurretYaw();
        float difference = Mth.degreesDifference(yaw, desiredYaw);
        float newYaw = yaw + Mth.clamp(difference, -traverseSpeed, traverseSpeed);
        float newPitch = Mth.approach(getBarrelPitch(), desiredPitch, traverseSpeed);

        if (Math.abs(difference) > 1.0F && tickCount % 10 == 0) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.TURRET_TRAVERSE.get(),
                    SoundSource.NEUTRAL, 0.6F, 1.0F);
        }
        this.entityData.set(DATA_TURRET_YAW, Mth.wrapDegrees(newYaw));
        this.entityData.set(DATA_BARREL_PITCH, Mth.clamp(newPitch, MIN_PITCH, MAX_PITCH));
    }

    public void setFiring(boolean firing) {
        this.entityData.set(DATA_FIRING, firing);
    }

    private void fireBurst(@Nullable LivingEntity gunner) {
        if (getAmmo() <= 0) {
            this.entityData.set(DATA_FIRING, false);
            if (tickCount % 40 == 0) {
                level().playSound(null, getX(), getY(), getZ(), ModSounds.REMOTE_ERROR.get(),
                        SoundSource.NEUTRAL, 1.0F, 0.7F);
                if (gunner instanceof Player player) {
                    player.displayClientMessage(
                            Component.translatable("message.airsystem.turret.no_ammo").withStyle(ChatFormatting.RED),
                            true);
                }
            }
            return;
        }

        fireCooldown = kind.getFireInterval();
        this.entityData.set(DATA_FIRING, true);
        setAmmo(getAmmo() - 1);
        barrelIndex = (barrelIndex + 1) % Math.max(1, kind.getBarrels());

        Vec3 muzzle = muzzlePosition();
        Vec3 direction = aimVector();
        double range = kind.getRange();

        boolean miss = gunner == null && autoTarget != null
                && random.nextFloat() > hitChance(autoTarget);

        double spread = miss
                ? 0.045D + random.nextDouble() * 0.07D
                : (1.0D - kind.getAccuracy()) * 0.06D;
        Vec3 shot = direction.add(
                (random.nextDouble() - 0.5D) * spread,
                (random.nextDouble() - 0.5D) * spread,
                (random.nextDouble() - 0.5D) * spread).normalize();

        Vec3 end = muzzle.add(shot.scale(range));
        Vec3 impact = end;

        DroneFlight hitDrone = miss ? null : traceDrone(muzzle, end);
        if (hitDrone != null && level() instanceof ServerLevel server) {
            impact = closestPointOnSegment(muzzle, end, hitDrone.position());
            hitDrone.hit(server, impact, true, gunner != null ? gunner : this);
        } else {
            BlockHitResult blockHit = level().clip(new ClipContext(muzzle, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != HitResult.Type.MISS) {
                impact = blockHit.getLocation();
            }
        }

        var sound = "35mm".equals(kind.getCaliber())
                ? ModSounds.TURRET_FIRE_35.get()
                : ModSounds.TURRET_FIRE_30.get();
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, 4.0F,
                0.95F + random.nextFloat() * 0.1F);

        if (level() instanceof ServerLevel server) {
            TracerPayload tracer = new TracerPayload(muzzle.x, muzzle.y, muzzle.z, impact.x, impact.y, impact.z);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(this, tracer);
            server.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 3, 0.05D, 0.05D, 0.05D, 0.02D);
            server.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 4, 0.1D, 0.1D, 0.1D, 0.01D);
        }

        if (getAmmo() == 0) {
            reloadTicks = 60;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.TURRET_RELOAD.get(),
                    SoundSource.NEUTRAL, 2.0F, 1.0F);
        }
    }

    @Nullable
    private DroneFlight traceDrone(Vec3 from, Vec3 to) {
        if (!(level() instanceof ServerLevel server)) {
            return null;
        }
        AABB path = new AABB(from, to).inflate(2.0D);
        DroneFlight best = null;
        double bestDistance = Double.MAX_VALUE;
        boolean automatic = getControllingPassenger() == null;
        for (DroneFlight drone : DroneFlightManager.get(server).inBox(path)) {
            if (automatic && isFriendly(drone)) {
                continue;
            }
            Vec3 closest = closestPointOnSegment(from, to, drone.position());
            double missDistance = closest.distanceTo(drone.position());

            double hitRadius = Math.max(drone.kind().getLength(), drone.kind().getWingspan()) * 0.45D;
            if (missDistance > hitRadius) {
                continue;
            }
            double distance = closest.distanceToSqr(from);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = drone;
            }
        }
        return best;
    }

    private static Vec3 closestPointOnSegment(Vec3 from, Vec3 to, Vec3 point) {
        Vec3 segment = to.subtract(from);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-6D) {
            return from;
        }
        double t = Mth.clamp(point.subtract(from).dot(segment) / lengthSqr, 0.0D, 1.0D);
        return from.add(segment.scale(t));
    }

    public Vec3 muzzlePosition() {
        float yawRad = (float) Math.toRadians(getTurretYaw());
        float pitchRad = (float) Math.toRadians(getBarrelPitch());
        double forward = kind.getLength() * 0.55D * Math.cos(pitchRad);
        return new Vec3(
                getX() - Math.sin(yawRad) * forward,
                getY() + kind.getHeight() * 0.7D + Math.sin(pitchRad) * kind.getLength() * 0.55D,
                getZ() + Math.cos(yawRad) * forward);
    }

    public Vec3 aimVector() {
        float yawRad = (float) Math.toRadians(getTurretYaw());
        float pitchRad = (float) Math.toRadians(getBarrelPitch());
        double horizontal = Math.cos(pitchRad);
        return new Vec3(-Math.sin(yawRad) * horizontal, Math.sin(pitchRad), Math.cos(yawRad) * horizontal);
    }

    private void spawnMuzzleParticles() {
        Vec3 muzzle = muzzlePosition();
        Vec3 aim = aimVector();
        for (int i = 0; i < 2; i++) {
            level().addParticle(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z,
                    aim.x * 0.4D, aim.y * 0.4D, aim.z * 0.4D);
        }
        level().addParticle(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 0.0D, 0.02D, 0.0D);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (TaczCompat.isAmmoFor(held, kind.getCaliber())) {
            if (level().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            int free = kind.getMagazine() - getAmmo();
            if (free <= 0) {
                player.displayClientMessage(Component.translatable("message.airsystem.turret.full"), true);
                return InteractionResult.CONSUME;
            }
            int perItem = TaczCompat.roundsPerItem(held);
            int items = Math.min(held.getCount(), Math.max(1, free / perItem));
            setAmmo(getAmmo() + items * perItem);
            if (!player.isCreative()) {
                held.shrink(items);
            }
            level().playSound(null, getX(), getY(), getZ(), ModSounds.TURRET_RELOAD.get(),
                    SoundSource.NEUTRAL, 1.5F, 1.0F);
            player.displayClientMessage(Component.translatable("message.airsystem.turret.loaded", getAmmo()), true);
            return InteractionResult.CONSUME;
        }

        if (kind.isAutoOnly()) {
            if (!level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.airsystem.turret.always_auto",
                        (int) AirSystemConfig.friendlyRadius()), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (player.isShiftKeyDown()) {
            if (level().isClientSide) {
                return InteractionResult.SUCCESS;
            }
            boolean auto = !isAutoMode();
            this.entityData.set(DATA_AUTO, auto);
            autoTarget = null;
            player.displayClientMessage(Component.translatable(auto
                    ? "message.airsystem.turret.auto_on"
                    : "message.airsystem.turret.auto_off"), true);
            return InteractionResult.CONSUME;
        }

        if (!level().isClientSide && getPassengers().isEmpty()) {
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof Player player && player.isCreative()) {
            destroyTurret(true);
            return true;
        }
        float health = getHealth() - amount;
        this.entityData.set(DATA_HEALTH, health);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + kind.getHeight() * 0.5D, getZ(),
                    6, 0.4D, 0.4D, 0.4D, 0.02D);
        }
        if (health <= 0.0F) {
            destroyTurret(false);
        }
        return true;
    }

    private void destroyTurret(boolean silent) {
        if (!silent && level() instanceof ServerLevel server) {
            com.rbxlu.airsystem.util.ImpactEffects.detonateWarhead(server, position(), 3.5F, true, 12, this);
        } else {
            spawnAtLocation(new ItemStack(ModItems.turretItem(kind).get()));
        }
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_TURRET_YAW, tag.getFloat("TurretYaw"));
        this.entityData.set(DATA_BARREL_PITCH, tag.getFloat("BarrelPitch"));
        this.entityData.set(DATA_AMMO, tag.getInt("Ammo"));
        this.entityData.set(DATA_AUTO, tag.getBoolean("AutoMode"));
        this.entityData.set(DATA_HEALTH, tag.contains("Health") ? tag.getFloat("Health") : MAX_HEALTH);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("TurretYaw", getTurretYaw());
        tag.putFloat("BarrelPitch", getBarrelPitch());
        tag.putInt("Ammo", getAmmo());
        tag.putBoolean("AutoMode", isAutoMode());
        tag.putFloat("Health", getHealth());
    }
}
