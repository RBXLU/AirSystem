package com.rbxlu.airsystem.client.handler;

import com.rbxlu.airsystem.util.AirSystemConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ScreenShakeHandler {
    private static final class Shake {
        private final Vec3 origin;
        private final float power;
        private final int duration;
        private int age;

        private Shake(Vec3 origin, float power, int duration) {
            this.origin = origin;
            this.power = power;
            this.duration = duration;
        }
    }

    private static final List<Shake> SHAKES = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    public static void add(Vec3 origin, float power) {
        int duration = Mth.clamp((int) (power * 8.0F), 14, 60);
        SHAKES.add(new Shake(origin, power, duration));
    }

    public static void tick() {
        Iterator<Shake> iterator = SHAKES.iterator();
        while (iterator.hasNext()) {
            Shake shake = iterator.next();
            if (++shake.age > shake.duration) {
                iterator.remove();
            }
        }
    }

    public static float currentIntensity() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || SHAKES.isEmpty()) {
            return 0.0F;
        }

        Vec3 viewer = minecraft.player.position();
        float total = 0.0F;
        for (Shake shake : SHAKES) {
            double distance = viewer.distanceTo(shake.origin);
            if (distance > AirSystemConfig.shakeRadius()) {
                continue;
            }

            float falloff = (float) (1.0D - distance / AirSystemConfig.shakeRadius());
            float decay = 1.0F - (shake.age / (float) shake.duration);
            total += shake.power * falloff * falloff * decay * 0.9F;
        }
        return Math.min(total, 14.0F);
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float intensity = currentIntensity();
        if (intensity <= 0.001F) {
            return;
        }
        event.setYaw(event.getYaw() + (RANDOM.nextFloat() - 0.5F) * intensity);
        event.setPitch(event.getPitch() + (RANDOM.nextFloat() - 0.5F) * intensity);
        event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5F) * intensity * 0.6F);
    }

    public static void clear() {
        SHAKES.clear();
    }

    private ScreenShakeHandler() {
    }
}
