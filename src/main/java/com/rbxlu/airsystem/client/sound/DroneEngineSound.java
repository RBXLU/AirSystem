package com.rbxlu.airsystem.client.sound;

import com.rbxlu.airsystem.client.ClientDroneStore;
import com.rbxlu.airsystem.content.drone.DroneKind;
import com.rbxlu.airsystem.content.drone.DroneState;
import com.rbxlu.airsystem.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class DroneEngineSound extends AbstractTickableSoundInstance {
    // Blocks per tick, i.e. 343 m/s at 20 tps.
    private static final float SOUND_SPEED = 17.15F;

    // Flight speeds here are compressed, so an honest shift would be inaudible.
    private static final float DOPPLER_GAIN = 3.0F;
    private static final float MAX_SHIFT = 0.35F;

    private final ClientDroneStore.ClientDrone drone;
    private final float basePitch;

    private float rpm = 1.0F;

    private DroneEngineSound(ClientDroneStore.ClientDrone drone) {
        super(soundFor(drone.kind()), SoundSource.NEUTRAL, RandomSource.create());
        this.drone = drone;
        this.basePitch = basePitchFor(drone.kind());
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.x = drone.position().x;
        this.y = drone.position().y;
        this.z = drone.position().z;
    }

    private static float basePitchFor(DroneKind kind) {
        return Mth.clamp((float) Math.pow(4.2F / Math.max(0.8F, kind.getLength()), 0.35F),
                0.75F, 1.5F);
    }

    private static net.minecraft.sounds.SoundEvent soundFor(DroneKind kind) {
        return switch (kind.getEngineSound()) {
            case JET -> ModSounds.DRONE_JET.get();
            case ELECTRIC -> ModSounds.DRONE_ELECTRIC.get();
            case PISTON -> ModSounds.DRONE_PISTON.get();
        };
    }

    public static void start(ClientDroneStore.ClientDrone drone) {
        Minecraft.getInstance().getSoundManager().play(new DroneEngineSound(drone));
    }

    @Override
    public void tick() {
        if (ClientDroneStore.byNetId(drone.netId()) != drone || !drone.state().isAirborne()) {
            stop();
            return;
        }

        Vec3 position = drone.position();
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;

        if (drone.engineDead()) {
            this.volume = 0.35F;
            this.pitch = basePitch * (0.7F + Mth.sin(drone.age() * 0.4F) * 0.15F);
            return;
        }

        float demand = 0.82F + 0.28F * Mth.clamp(drone.throttle(), 0.0F, 1.2F);
        if (drone.state() == DroneState.DIVE) {
            demand += 0.12F;
        }
        rpm += (demand - rpm) * 0.15F;

        this.volume = 0.75F + 0.25F * Mth.clamp(drone.throttle(), 0.0F, 1.0F);
        this.pitch = Mth.clamp(basePitch * rpm * doppler(position), 0.5F, 2.0F);
    }

    private float doppler(Vec3 position) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return 1.0F;
        }
        Vec3 toListener = client.player.getEyePosition().subtract(position);
        double distance = toListener.length();
        if (distance < 1.0E-3D) {
            return 1.0F;
        }

        double closing = drone.velocity().dot(toListener.scale(1.0D / distance));
        float shift = (float) (closing * DOPPLER_GAIN / SOUND_SPEED);
        return 1.0F + Mth.clamp(shift, -MAX_SHIFT, MAX_SHIFT);
    }

    @Override
    public boolean canPlaySound() {
        return true;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
