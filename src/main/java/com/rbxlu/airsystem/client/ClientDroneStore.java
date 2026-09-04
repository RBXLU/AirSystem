package com.rbxlu.airsystem.client;

import com.rbxlu.airsystem.client.sound.DroneEngineSound;
import com.rbxlu.airsystem.content.drone.DroneKind;
import com.rbxlu.airsystem.content.drone.DroneState;
import com.rbxlu.airsystem.network.payload.DroneSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ClientDroneStore {
    private static final int STALE_TICKS = 60;

    // Snapshots arrive unevenly; easing over a few ticks stops the model stuttering.
    private static final int LERP_STEPS = 3;

    private static final double TRAIL_DISTANCE = 96.0D;

    public static final class ClientDrone {
        private final int netId;
        private DroneKind kind;
        private DroneState state = DroneState.CRUISE;
        private boolean engineDead;
        private boolean manual;
        private float throttle = 1.0F;

        private Vec3 previous;
        private Vec3 current;
        private float previousYaw;
        private float yaw;
        private float previousPitch;
        private float pitch;
        private float previousRoll;
        private float roll;

        private Vec3 target;
        private float targetYaw;
        private float targetPitch;
        private float targetRoll;
        private int lerpSteps;

        private float propellerSpin;
        private double speed;
        private int age;
        private int silentTicks;

        private ClientDrone(int netId, DroneKind kind, Vec3 position) {
            this.netId = netId;
            this.kind = kind;
            this.previous = position;
            this.current = position;
            this.target = position;
        }

        public int netId() {
            return netId;
        }

        public DroneKind kind() {
            return kind;
        }

        public DroneState state() {
            return state;
        }

        public boolean engineDead() {
            return engineDead;
        }

        public boolean manual() {
            return manual;
        }

        public float propellerSpin() {
            return propellerSpin;
        }

        public int age() {
            return age;
        }

        public Vec3 position() {
            return current;
        }

        public Vec3 lerpPosition(float partialTick) {
            return previous.lerp(current, partialTick);
        }

        public float lerpYaw(float partialTick) {
            return Mth.rotLerp(partialTick, previousYaw, yaw);
        }

        public float lerpPitch(float partialTick) {
            return Mth.rotLerp(partialTick, previousPitch, pitch);
        }

        public float lerpRoll(float partialTick) {
            return Mth.rotLerp(partialTick, previousRoll, roll);
        }

        public double speed() {
            return speed;
        }

        public float throttle() {
            return throttle;
        }

        public Vec3 velocity() {
            return current.subtract(previous);
        }

        private void accept(DroneSyncPayload.Snapshot snapshot) {
            kind = snapshot.kind();
            state = snapshot.state();
            engineDead = snapshot.engineDead();
            manual = snapshot.manual();
            throttle = snapshot.throttle();

            Vec3 incoming = new Vec3(snapshot.x(), snapshot.y(), snapshot.z());
            speed = incoming.distanceTo(target);

            // Teleport or entry into view range: snap, do not glide in from far away.
            if (incoming.distanceToSqr(current) > 4096.0D) {
                previous = incoming;
                current = incoming;
                previousYaw = yaw = snapshot.yaw();
                previousPitch = pitch = snapshot.pitch();
                previousRoll = roll = snapshot.roll();
                lerpSteps = 0;
            } else {
                lerpSteps = LERP_STEPS;
            }

            target = incoming;
            targetYaw = snapshot.yaw();
            targetPitch = snapshot.pitch();
            targetRoll = snapshot.roll();
            silentTicks = 0;
        }

        private void tick() {
            age++;
            silentTicks++;
            advance();
            if (!state.isAirborne()) {
                return;
            }
            float rate = switch (kind.getEngineSound()) {
                case JET -> 3.6F;
                case PISTON -> 2.4F;
                case ELECTRIC -> 3.0F;
            };
            propellerSpin = (propellerSpin + rate * 6.0F * (engineDead ? 0.15F : throttle)) % 360.0F;
        }

        private void advance() {
            previous = current;
            previousYaw = yaw;
            previousPitch = pitch;
            previousRoll = roll;

            if (lerpSteps <= 0) {
                current = target;
                yaw = targetYaw;
                pitch = targetPitch;
                roll = targetRoll;
                return;
            }

            double factor = 1.0D / lerpSteps;
            current = current.add(target.subtract(current).scale(factor));
            yaw = yaw + Mth.degreesDifference(yaw, targetYaw) * (float) factor;
            pitch = pitch + Mth.degreesDifference(pitch, targetPitch) * (float) factor;
            roll = roll + Mth.degreesDifference(roll, targetRoll) * (float) factor;
            lerpSteps--;
        }
    }

    public static final class ClientMunition {
        private final int netId;
        private Vec3 previous;
        private Vec3 current;
        private float yaw;
        private float pitch;
        private int age;
        private int silentTicks;

        private ClientMunition(int netId, Vec3 position) {
            this.netId = netId;
            this.previous = position;
            this.current = position;
        }

        public Vec3 lerpPosition(float partialTick) {
            return previous.lerp(current, partialTick);
        }

        public float yaw() {
            return yaw;
        }

        public float pitch() {
            return pitch;
        }

        public int age() {
            return age;
        }

        public Vec3 position() {
            return current;
        }
    }

    private static final Map<Integer, ClientDrone> DRONES = new HashMap<>();
    private static final Map<Integer, ClientMunition> MUNITIONS = new HashMap<>();

    public static Collection<ClientDrone> drones() {
        return DRONES.values();
    }

    public static Collection<ClientMunition> munitions() {
        return MUNITIONS.values();
    }

    @Nullable
    public static ClientDrone byNetId(int netId) {
        return DRONES.get(netId);
    }

    public static void accept(DroneSyncPayload payload) {
        for (DroneSyncPayload.Snapshot snapshot : payload.drones()) {
            ClientDrone drone = DRONES.computeIfAbsent(snapshot.netId(), id -> {
                ClientDrone created = new ClientDrone(id, snapshot.kind(),
                        new Vec3(snapshot.x(), snapshot.y(), snapshot.z()));
                DroneEngineSound.start(created);
                return created;
            });
            drone.accept(snapshot);
        }

        for (DroneSyncPayload.MunitionSnapshot snapshot : payload.munitions()) {
            ClientMunition munition = MUNITIONS.computeIfAbsent(snapshot.netId(),
                    id -> new ClientMunition(id, new Vec3(snapshot.x(), snapshot.y(), snapshot.z())));
            munition.previous = munition.current;
            Vec3 incoming = new Vec3(snapshot.x(), snapshot.y(), snapshot.z());
            if (incoming.distanceToSqr(munition.current) > 4096.0D) {
                munition.previous = incoming;
            }
            munition.current = incoming;
            munition.yaw = snapshot.yaw();
            munition.pitch = snapshot.pitch();
            munition.silentTicks = 0;
        }
    }

    public static void remove(List<Integer> netIds) {
        for (Integer netId : netIds) {
            DRONES.remove(netId);
            MUNITIONS.remove(netId);
        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            clear();
            return;
        }

        Iterator<ClientDrone> iterator = DRONES.values().iterator();
        List<ClientDrone> active = new ArrayList<>();
        while (iterator.hasNext()) {
            ClientDrone drone = iterator.next();
            drone.tick();
            if (drone.silentTicks > STALE_TICKS) {
                iterator.remove();
                continue;
            }
            active.add(drone);
        }

        Vec3 viewer = minecraft.gameRenderer.getMainCamera().getPosition();
        for (ClientDrone drone : active) {
            if (drone.current.distanceToSqr(viewer) <= TRAIL_DISTANCE * TRAIL_DISTANCE) {
                spawnTrail(minecraft, drone);
            }
        }

        MUNITIONS.values().removeIf(munition -> {
            munition.age++;
            munition.silentTicks++;
            if (munition.silentTicks > STALE_TICKS) {
                return true;
            }
            if (munition.age % 2 == 0) {
                minecraft.level.addParticle(ParticleTypes.SMOKE,
                        munition.current.x, munition.current.y, munition.current.z, 0.0D, 0.0D, 0.0D);
            }
            return false;
        });
    }

    private static void spawnTrail(Minecraft minecraft, ClientDrone drone) {
        if (minecraft.level == null || !drone.state.isAirborne()) {
            return;
        }

        float yawRad = (float) Math.toRadians(drone.yaw);
        float pitchRad = (float) Math.toRadians(drone.pitch);
        double horizontal = Math.cos(pitchRad);
        Vec3 back = new Vec3(Math.sin(yawRad) * horizontal, Math.sin(pitchRad),
                -Math.cos(yawRad) * horizontal).scale(drone.kind.getLength() * 0.5D);
        double x = drone.current.x + back.x;
        double y = drone.current.y + back.y;
        double z = drone.current.z + back.z;
        var random = minecraft.level.random;

        if (drone.engineDead) {
            for (int i = 0; i < 3; i++) {
                minecraft.level.addParticle(ParticleTypes.LARGE_SMOKE,
                        x + (random.nextDouble() - 0.5D), y + (random.nextDouble() - 0.5D),
                        z + (random.nextDouble() - 0.5D), 0.0D, 0.02D, 0.0D);
            }
            if (random.nextInt(3) == 0) {
                minecraft.level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
            }
            return;
        }

        if (drone.kind.getEngineSound() == DroneKind.EngineSound.JET) {
            minecraft.level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
            if (random.nextBoolean()) {
                minecraft.level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
            }
        } else if (drone.kind.getEngineSound() == DroneKind.EngineSound.PISTON && random.nextInt(6) == 0) {
            minecraft.level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    public static void clear() {
        DRONES.clear();
        MUNITIONS.clear();
    }

    private ClientDroneStore() {
    }
}
