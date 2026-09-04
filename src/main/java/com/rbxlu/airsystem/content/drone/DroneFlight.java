package com.rbxlu.airsystem.content.drone;

import com.rbxlu.airsystem.registry.ModSounds;
import com.rbxlu.airsystem.util.AirSystemConfig;
import com.rbxlu.airsystem.util.ImpactEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;
import java.util.UUID;

public class DroneFlight {
    public static final int INPUT_FORWARD = 1;
    public static final int INPUT_LEFT = 1 << 1;
    public static final int INPUT_BACK = 1 << 2;
    public static final int INPUT_RIGHT = 1 << 3;
    public static final int INPUT_BOOST = 1 << 4;
    public static final int INPUT_BRAKE = 1 << 5;

    private final UUID id;
    private final DroneKind kind;
    private final int netId;

    private Vec3 position;
    private Vec3 velocity = Vec3.ZERO;
    private float yaw;
    private float pitch;
    private float roll;

    private DroneState state = DroneState.LAUNCH;
    private BlockPos target = BlockPos.ZERO;
    private int cruiseAltitude;
    private int fuel;
    private int coreHits;
    private int wingHits;
    private boolean engineDead;
    private boolean manual;
    private int input;
    private float throttle = 1.0F;
    private int munitions = 4;
    private int fallTicks;
    private int age;

    private BlockPos home = BlockPos.ZERO;

    @Nullable
    private BlockPos landingPad;
    private boolean landingRequested;
    @Nullable
    private UUID operator;

    public DroneFlight(UUID id, int netId, DroneKind kind, Vec3 position, float yaw) {
        this.id = id;
        this.netId = netId;
        this.kind = kind;
        this.position = position;
        this.yaw = yaw;
        this.fuel = AirSystemConfig.droneFlightTicks();
    }

    public UUID id() {
        return id;
    }

    public int netId() {
        return netId;
    }

    public DroneKind kind() {
        return kind;
    }

    public Vec3 position() {
        return position;
    }

    public Vec3 velocity() {
        return velocity;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float roll() {
        return roll;
    }

    public DroneState state() {
        return state;
    }

    public BlockPos target() {
        return target;
    }

    public boolean hasTarget() {
        return !BlockPos.ZERO.equals(target);
    }

    public int fuel() {
        return fuel;
    }

    public int coreHits() {
        return coreHits;
    }

    public boolean engineDead() {
        return engineDead;
    }

    public boolean manual() {
        return manual;
    }

    public int munitions() {
        return munitions;
    }

    public float throttle() {
        return throttle;
    }

    @Nullable
    public UUID operator() {
        return operator;
    }

    public void setOperator(@Nullable UUID operator) {
        this.operator = operator;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
        this.input = 0;
    }

    public void setInput(int input) {
        this.input = input;
    }

    public void setTarget(BlockPos target) {
        this.target = target;
        this.cruiseAltitude = 0;
    }

    public void setState(DroneState state) {
        this.state = state;
    }

    public BlockPos home() {
        return home;
    }

    public void setHome(BlockPos home) {
        this.home = home;
    }

    public boolean hasHome() {
        return !BlockPos.ZERO.equals(home);
    }

    public boolean recovering() {
        return state.isRecovering();
    }

    public boolean canLand() {
        return kind.canRecover() && !engineDead && hasHome() && state.isControllable();
    }

    public boolean isAlive() {
        return !state.isFinished();
    }

    public AABB box() {
        double half = Math.max(kind.getLength(), kind.getWingspan()) * 0.36D;
        double height = Math.max(0.4D, kind.getLength() * 0.14D);
        return new AABB(position.x - half, position.y - height, position.z - half,
                position.x + half, position.y + height, position.z + half);
    }

    public Vec3 forward() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        double horizontal = Math.cos(pitchRad);
        return new Vec3(-Math.sin(yawRad) * horizontal, -Math.sin(pitchRad), Math.cos(yawRad) * horizontal);
    }

    public boolean tick(ServerLevel level, DroneFlightManager manager) {
        age++;

        switch (state) {
            case LAUNCH -> tickLaunch(level);
            case CRUISE -> tickCruise(level);
            case ORBIT -> tickOrbit(level);
            case DIVE -> tickDive(level);
            case RTB -> tickReturn(level);
            case LANDING -> tickLanding(level);
            case FALLING -> tickFalling(level);
            default -> {
            }
        }

        if (state.isControllable()) {
            fuel--;
            if (fuel <= 0 && !state.isRecovering()) {
                if (canLand()) {
                    beginReturn(level);
                } else {
                    failEngine(level, null);
                }
            } else if (fuel > 0 && !state.isRecovering() && (landingRequested || needsReturn())) {
                beginReturn(level);
            }
        }

        if (age % 40 == 0 && state.isAirborne()) {
            emitEngineSound(level);
        }
        return state.isFinished();
    }

    private boolean needsReturn() {
        if (!kind.canRecover() || !hasHome() || engineDead) {
            return false;
        }
        double distance = Math.sqrt(home.distToCenterSqr(position));
        int needed = (int) (distance / Math.max(0.2D, kind.getCruiseSpeed())) + 220;
        return fuel <= needed;
    }

    private void tickLaunch(ServerLevel level) {
        pitch = approach(pitch, -18.0F, 2.0F);
        advance(level, kind.getCruiseSpeed() * 0.75F * throttle);
        if (position.y >= cruiseAltitude(level) - 2) {
            state = DroneState.CRUISE;
        }
    }

    private void tickCruise(ServerLevel level) {
        if (manual) {
            applyManualInput();
            advance(level, kind.getCruiseSpeed() * throttle);
            return;
        }
        if (!hasTarget()) {
            pitch = approach(pitch, 0.0F, 1.0F);
            advance(level, kind.getCruiseSpeed() * throttle);
            return;
        }

        double dx = target.getX() + 0.5D - position.x;
        double dz = target.getZ() + 0.5D - position.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        steer((float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F, turnRate());

        double altitudeError = cruiseAltitude(level) - position.y;
        pitch = approach(pitch, (float) Mth.clamp(-altitudeError * 1.4D, -25.0D, 20.0D), 1.5F);
        advance(level, kind.getCruiseSpeed() * throttle);

        double altitude = position.y - target.getY();
        boolean kamikaze = kind.getRole() == DroneKind.Role.KAMIKAZE && hasTarget();
        if (kamikaze && horizontal < altitude * 1.8D + 12.0D) {
            state = DroneState.DIVE;
            level.playSound(null, position.x, position.y, position.z, ModSounds.DRONE_DIVE.get(),
                    SoundSource.NEUTRAL, 6.0F, 1.0F);
        } else if (!kamikaze && hasTarget() && horizontal < 30.0D) {
            state = DroneState.ORBIT;
        }
    }

    private void tickOrbit(ServerLevel level) {
        if (manual) {
            applyManualInput();
            advance(level, kind.getCruiseSpeed() * throttle);
            return;
        }

        double dx = position.x - (target.getX() + 0.5D);
        double dz = position.z - (target.getZ() + 0.5D);
        double radius = Math.sqrt(dx * dx + dz * dz);

        double heading = Mth.atan2(dz, dx) + Math.PI / 2.0D - (radius - 26.0D) * 0.02D;
        steer((float) (heading * (180.0D / Math.PI)) - 90.0F, turnRate());

        double altitudeError = cruiseAltitude(level) - position.y;
        pitch = approach(pitch, (float) Mth.clamp(-altitudeError * 1.4D, -20.0D, 20.0D), 1.5F);
        advance(level, kind.getCruiseSpeed() * 0.8F * throttle);
    }

    private void tickDive(ServerLevel level) {
        Vec3 aim = Vec3.atCenterOf(target);
        Vec3 delta = aim.subtract(position);

        if (manual) {
            applyManualInput();
        } else {
            double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            steer((float) (Mth.atan2(delta.z, delta.x) * (180.0D / Math.PI)) - 90.0F, 6.0F);
            pitch = approach(pitch, (float) (-Mth.atan2(delta.y, horizontal) * (180.0D / Math.PI)), 5.0F);
        }

        if (advance(level, kind.getCruiseSpeed() * 1.45F * throttle)) {
            return;
        }
        if (delta.lengthSqr() < 2.25D) {
            detonate(level, position);
        }
    }

    private void tickReturn(ServerLevel level) {
        BlockPos pad = landingPad != null ? landingPad : home;
        double dx = pad.getX() + 0.5D - position.x;
        double dz = pad.getZ() + 0.5D - position.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        steer((float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F, turnRate());

        double patternAltitude = pad.getY() + 24.0D;
        double altitudeError = patternAltitude - position.y;
        pitch = approach(pitch, (float) Mth.clamp(-altitudeError * 1.2D, -22.0D, 18.0D), 1.5F);
        advance(level, kind.getCruiseSpeed() * 0.85F * throttle);

        if (horizontal < 26.0D && Math.abs(altitudeError) < 10.0D) {
            landingPad = findLandingPad(level);
            state = DroneState.LANDING;
            level.playSound(null, position.x, position.y, position.z, ModSounds.REMOTE_BEEP.get(),
                    SoundSource.NEUTRAL, 2.0F, 0.8F);
        }
    }

    private void tickLanding(ServerLevel level) {
        BlockPos pad = landingPad != null ? landingPad : home;
        double dx = pad.getX() + 0.5D - position.x;
        double dz = pad.getZ() + 0.5D - position.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double height = position.y - (pad.getY() + 0.6D);

        if (horizontal > 3.0D) {
            steer((float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F, turnRate() * 1.4F);
            float glide = (float) Mth.clamp(
                    Math.toDegrees(Math.atan2(height, Math.max(2.5D, horizontal))), 0.0D, 26.0D);
            pitch = approach(pitch, glide, 3.0F);
            roll = Mth.lerp(0.25F, roll, 0.0F);
            advance(level, kind.getApproachSpeed());
            return;
        }

        pitch = approach(pitch, 0.0F, 4.0F);
        roll = Mth.lerp(0.3F, roll, 0.0F);
        Vec3 descent = new Vec3(dx * 0.18D, -Math.min(0.28D, Math.max(0.08D, height * 0.35D)),
                dz * 0.18D);
        velocity = descent;
        if (move(level, descent)) {
            return;
        }

        if (position.y - (pad.getY() + 0.6D) <= 0.35D) {
            touchDown(level, pad);
        }
    }

    private BlockPos findLandingPad(ServerLevel level) {
        if (isPadFree(level, home)) {
            return home;
        }
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            home.offset(dx, 0, dz));
                    if (isPadFree(level, surface)) {
                        return surface;
                    }
                }
            }
        }
        return home;
    }

    private static boolean isPadFree(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.below()).isSolid();
    }

    private void touchDown(ServerLevel level, BlockPos pad) {
        if (state.isFinished()) {
            return;
        }
        BlockPos spot = isPadFree(level, pad) ? pad
                : level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pad);

        if (!isPadFree(level, spot)) {
            state = DroneState.DESTROYED;
            ImpactEffects.crashWreck(level, position, null);
            return;
        }

        state = DroneState.LANDED;
        position = Vec3.atBottomCenterOf(spot);

        var facing = net.minecraft.core.Direction.fromYRot(yaw);
        level.setBlock(spot, com.rbxlu.airsystem.registry.ModBlocks.DRONE.get().defaultBlockState()
                .setValue(DroneBlock.FACING, facing), Block.UPDATE_ALL);
        if (level.getBlockEntity(spot) instanceof DroneBlockEntity stand) {
            stand.setKind(kind);
            stand.setTarget(target);
        }
        level.playSound(null, spot, ModSounds.DRONE_LAUNCH.get(), SoundSource.NEUTRAL, 2.0F, 0.7F);
    }

    public boolean beginReturn(ServerLevel level) {
        if (!canLand()) {
            return false;
        }
        landingRequested = true;
        manual = false;
        input = 0;
        landingPad = null;
        state = DroneState.RTB;
        level.playSound(null, position.x, position.y, position.z, ModSounds.REMOTE_BEEP.get(),
                SoundSource.NEUTRAL, 2.0F, 1.2F);
        return true;
    }

    private void tickFalling(ServerLevel level) {
        fallTicks++;
        yaw += 7.0F + Math.min(fallTicks * 0.25F, 12.0F);
        roll = (roll + 11.0F) % 360.0F;
        pitch = approach(pitch, 65.0F, 2.5F);

        velocity = velocity.multiply(0.96D, 1.0D, 0.96D).add(0.0D, -0.06D, 0.0D);
        move(level, velocity);
    }

    private float turnRate() {
        return kind.getTurnRate() * 110.0F;
    }

    private boolean advance(ServerLevel level, float speed) {
        velocity = forward().scale(speed);

        return move(level, velocity);
    }

    private boolean move(ServerLevel level, Vec3 motion) {
        Vec3 next = position.add(motion);
        BlockHitResult hit = level.clip(new ClipContext(position, next,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));

        if (hit.getType() != HitResult.Type.MISS) {
            position = hit.getLocation();
            detonate(level, position);
            return true;
        }

        position = next;
        if (position.y < level.getMinBuildHeight() - 8 || position.y > level.getMaxBuildHeight() + 256) {
            detonate(level, position);
            return true;
        }
        return false;
    }

    private void applyManualInput() {
        float turn = turnRate();
        if ((input & INPUT_LEFT) != 0) {
            yaw -= turn;
        }
        if ((input & INPUT_RIGHT) != 0) {
            yaw += turn;
        }
        if ((input & INPUT_FORWARD) != 0) {
            pitch = Mth.clamp(pitch + 2.0F, -60.0F, 75.0F);
        }
        if ((input & INPUT_BACK) != 0) {
            pitch = Mth.clamp(pitch - 2.0F, -60.0F, 75.0F);
        }
        if ((input & INPUT_BOOST) != 0) {
            throttle = Mth.clamp(throttle + 0.05F, 0.0F, 1.6F);
        }
        if ((input & INPUT_BRAKE) != 0) {
            throttle = Mth.clamp(throttle - 0.05F, 0.0F, 1.6F);
        }

        if ((input & (INPUT_FORWARD | INPUT_BACK)) == 0) {
            pitch = approach(pitch, 0.0F, 0.8F);
        }
        roll = Mth.lerp(0.25F, roll, ((input & INPUT_LEFT) != 0 ? -28.0F
                : (input & INPUT_RIGHT) != 0 ? 28.0F : 0.0F));
    }

    private void steer(float desiredYaw, float maxDegrees) {
        float difference = Mth.degreesDifference(yaw, desiredYaw);
        float step = Mth.clamp(difference, -maxDegrees, maxDegrees);
        yaw += step;
        roll = Mth.lerp(0.2F, roll, Mth.clamp(-step * 4.0F, -45.0F, 45.0F));
    }

    private static float approach(float current, float desired, float step) {
        float difference = Mth.degreesDifference(current, desired);
        return current + Mth.clamp(difference, -step, step);
    }

    private int cruiseAltitude(ServerLevel level) {
        if (cruiseAltitude == 0) {
            int base = hasTarget() ? target.getY() : (int) position.y;
            cruiseAltitude = Math.min(kind.getCeiling(), base + AirSystemConfig.cruiseAltitude());
        }
        return cruiseAltitude;
    }

    public void setCruiseAltitude(int altitude) {
        this.cruiseAltitude = altitude;
    }

    private void emitEngineSound(ServerLevel level) {
        if (age % 200 != 0) {
            return;
        }
        var sound = switch (kind.getEngineSound()) {
            case JET -> ModSounds.DRONE_JET.get();
            case ELECTRIC -> ModSounds.DRONE_ELECTRIC.get();
            case PISTON -> ModSounds.DRONE_PISTON.get();
        };
        level.playSound(null, position.x, position.y, position.z, sound, SoundSource.NEUTRAL, 3.0F, 1.0F);
    }

    public HitZone classifyHit(Vec3 worldPoint) {
        Vec3 local = worldPoint.subtract(position);
        float yawRad = (float) Math.toRadians(-yaw);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        double localX = local.x * cos - local.z * sin;
        double localZ = local.x * sin + local.z * cos;

        double halfLength = kind.getLength() * 0.5D;
        double halfSpan = kind.getWingspan() * 0.5D;

        if (Math.abs(localZ) > halfLength * 1.35D || Math.abs(localX) > halfSpan * 1.35D) {
            return HitZone.GRAZE;
        }
        if (localZ < -halfLength * 0.35D) {
            return HitZone.ENGINE;
        }
        if (Math.abs(localX) > halfSpan * 0.35D) {
            return HitZone.WING;
        }
        return HitZone.CORE;
    }

    public boolean hit(ServerLevel level, Vec3 impact, boolean fromGun, @Nullable Entity attacker) {
        if (state.isFinished()) {
            return false;
        }

        HitZone zone = classifyHit(impact);
        if (zone == HitZone.GRAZE && !fromGun) {
            return false;
        }

        ImpactEffects.hitSparks(level, impact);

        float failureChance = switch (zone) {
            case CORE -> AirSystemConfig.engineFailureChance();
            case ENGINE -> AirSystemConfig.engineFailureChance() * 2.0F;
            case WING -> AirSystemConfig.engineFailureChance() * 0.4F;
            case GRAZE -> 0.0F;
        };

        switch (zone) {
            case CORE -> {
                if (++coreHits >= kind.getCoreHits()) {
                    detonate(level, position);
                    return true;
                }
            }
            case WING -> wingHits++;
            default -> {
            }
        }

        if (!engineDead && state.isAirborne() && level.random.nextFloat() < failureChance) {
            failEngine(level, attacker);
        } else if (wingHits >= 4 && state.isAirborne() && level.random.nextFloat() < 0.5F) {
            failEngine(level, attacker);
        }
        return true;
    }

    public void failEngine(ServerLevel level, @Nullable Entity attacker) {
        if (engineDead) {
            return;
        }
        engineDead = true;
        manual = false;
        state = DroneState.FALLING;
        fallTicks = 0;
        level.playSound(null, position.x, position.y, position.z, ModSounds.ENGINE_FAILURE.get(),
                SoundSource.NEUTRAL, 5.0F, 1.0F);
    }

    public void detonate(ServerLevel level, Vec3 at) {
        if (state.isFinished()) {
            return;
        }
        state = DroneState.DESTROYED;
        if (kind.hasWarhead()) {
            ImpactEffects.detonateWarhead(level, at, kind.getWarheadPower(), kind.isIncendiary(),
                    kind.getWindowBreakRadius(), null);
        } else {
            ImpactEffects.crashWreck(level, at, null);
        }
    }

    @Nullable
    public Munition releaseMunition(int netId) {
        if (kind.getRole() != DroneKind.Role.STRIKE || munitions <= 0 || !state.isAirborne()) {
            return null;
        }
        munitions--;
        Vec3 aim = hasTarget() ? Vec3.atCenterOf(target) : null;
        return new Munition(netId, position.subtract(0.0D, 0.6D, 0.0D), velocity.scale(0.9D),
                yaw, pitch, aim);
    }

    public boolean isOperator(ServerPlayer player) {
        return operator == null || operator.equals(player.getUUID()) || player.hasPermissions(2);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Kind", kind.getId());
        tag.putDouble("X", position.x);
        tag.putDouble("Y", position.y);
        tag.putDouble("Z", position.z);
        tag.putDouble("VX", velocity.x);
        tag.putDouble("VY", velocity.y);
        tag.putDouble("VZ", velocity.z);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        tag.putFloat("Roll", roll);
        tag.putInt("State", state.ordinal());
        tag.putInt("TargetX", target.getX());
        tag.putInt("TargetY", target.getY());
        tag.putInt("TargetZ", target.getZ());
        tag.putInt("CruiseAltitude", cruiseAltitude);
        tag.putInt("Fuel", fuel);
        tag.putInt("CoreHits", coreHits);
        tag.putInt("WingHits", wingHits);
        tag.putBoolean("EngineDead", engineDead);
        tag.putBoolean("Manual", manual);
        tag.putFloat("Throttle", throttle);
        tag.putInt("Munitions", munitions);
        tag.putInt("HomeX", home.getX());
        tag.putInt("HomeY", home.getY());
        tag.putInt("HomeZ", home.getZ());
        tag.putBoolean("LandingRequested", landingRequested);
        if (landingPad != null) {
            tag.putInt("PadX", landingPad.getX());
            tag.putInt("PadY", landingPad.getY());
            tag.putInt("PadZ", landingPad.getZ());
        }
        if (operator != null) {
            tag.putUUID("Operator", operator);
        }
        return tag;
    }

    public static DroneFlight load(CompoundTag tag, int netId) {
        DroneFlight flight = new DroneFlight(
                tag.hasUUID("Id") ? tag.getUUID("Id") : UUID.randomUUID(),
                netId,
                DroneKind.byId(tag.getString("Kind")),
                new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")),
                tag.getFloat("Yaw"));

        flight.velocity = new Vec3(tag.getDouble("VX"), tag.getDouble("VY"), tag.getDouble("VZ"));
        flight.pitch = tag.getFloat("Pitch");
        flight.roll = tag.getFloat("Roll");
        flight.state = DroneState.byOrdinal(tag.getInt("State"));
        flight.target = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
        flight.cruiseAltitude = tag.getInt("CruiseAltitude");
        flight.fuel = tag.getInt("Fuel");
        flight.coreHits = tag.getInt("CoreHits");
        flight.wingHits = tag.getInt("WingHits");
        flight.engineDead = tag.getBoolean("EngineDead");
        flight.manual = tag.getBoolean("Manual");
        flight.throttle = tag.contains("Throttle") ? tag.getFloat("Throttle") : 1.0F;
        flight.munitions = tag.contains("Munitions") ? tag.getInt("Munitions") : 4;
        flight.home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
        flight.landingRequested = tag.getBoolean("LandingRequested");
        if (tag.contains("PadX")) {
            flight.landingPad = new BlockPos(tag.getInt("PadX"), tag.getInt("PadY"), tag.getInt("PadZ"));
        }
        if (tag.hasUUID("Operator")) {
            flight.operator = tag.getUUID("Operator");
        }
        return flight;
    }
}
