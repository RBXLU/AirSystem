package com.rbxlu.airsystem.content.drone;

import com.rbxlu.airsystem.util.ImpactEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;

public class Munition {
    private static final float POWER = 5.5F;
    private static final int WINDOW_RADIUS = 26;

    private final int netId;
    private Vec3 position;
    private Vec3 velocity;
    private float yaw;
    private float pitch;
    @Nullable
    private final Vec3 aimPoint;
    private int age;

    public Munition(int netId, Vec3 position, Vec3 velocity, float yaw, float pitch,
                    @Nullable Vec3 aimPoint) {
        this.netId = netId;
        this.position = position;
        this.velocity = velocity;
        this.yaw = yaw;
        this.pitch = pitch;
        this.aimPoint = aimPoint;
    }

    public int netId() {
        return netId;
    }

    public Vec3 position() {
        return position;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public boolean tick(ServerLevel level) {
        age++;

        Vec3 motion = velocity.add(0.0D, -0.055D, 0.0D).multiply(0.995D, 0.99D, 0.995D);
        if (aimPoint != null) {
            Vec3 toTarget = aimPoint.subtract(position);
            if (toTarget.horizontalDistance() > 0.5D) {
                motion = motion.add(new Vec3(toTarget.x, 0.0D, toTarget.z).normalize().scale(0.035D));
            }
        }
        velocity = motion;

        Vec3 next = position.add(motion);
        BlockHitResult hit = level.clip(new ClipContext(position, next,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() != HitResult.Type.MISS) {
            ImpactEffects.detonateWarhead(level, hit.getLocation(), POWER, false, WINDOW_RADIUS, null);
            return true;
        }

        position = next;
        yaw = (float) (Mth.atan2(motion.z, motion.x) * (180.0D / Math.PI)) - 90.0F;
        pitch = (float) (-Mth.atan2(motion.y, motion.horizontalDistance()) * (180.0D / Math.PI));

        if (age > 600 || position.y < level.getMinBuildHeight() - 8) {
            ImpactEffects.detonateWarhead(level, position, POWER, false, WINDOW_RADIUS, null);
            return true;
        }
        return false;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("X", position.x);
        tag.putDouble("Y", position.y);
        tag.putDouble("Z", position.z);
        tag.putDouble("VX", velocity.x);
        tag.putDouble("VY", velocity.y);
        tag.putDouble("VZ", velocity.z);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        tag.putInt("Age", age);
        if (aimPoint != null) {
            tag.putDouble("AimX", aimPoint.x);
            tag.putDouble("AimY", aimPoint.y);
            tag.putDouble("AimZ", aimPoint.z);
        }
        return tag;
    }

    public static Munition load(CompoundTag tag, int netId) {
        Vec3 aim = tag.contains("AimX")
                ? new Vec3(tag.getDouble("AimX"), tag.getDouble("AimY"), tag.getDouble("AimZ"))
                : null;
        Munition munition = new Munition(netId,
                new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")),
                new Vec3(tag.getDouble("VX"), tag.getDouble("VY"), tag.getDouble("VZ")),
                tag.getFloat("Yaw"), tag.getFloat("Pitch"), aim);
        munition.age = tag.getInt("Age");
        return munition;
    }
}
