package com.rbxlu.airsystem.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TracerRenderer {
    private static final int LIFETIME = 5;

    private static final class Tracer {
        private final Vec3 from;
        private final Vec3 to;
        private int age;

        private Tracer(Vec3 from, Vec3 to) {
            this.from = from;
            this.to = to;
        }
    }

    private static final List<Tracer> TRACERS = new ArrayList<>();

    public static void add(Vec3 from, Vec3 to) {
        TRACERS.add(new Tracer(from, to));
        if (TRACERS.size() > 256) {
            TRACERS.remove(0);
        }
    }

    public static void tick() {
        Iterator<Tracer> iterator = TRACERS.iterator();
        while (iterator.hasNext()) {
            if (++iterator.next().age > LIFETIME) {
                iterator.remove();
            }
        }
    }

    public static void render(RenderLevelStageEvent event) {
        if (TRACERS.isEmpty() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();

        for (Tracer tracer : TRACERS) {
            float alpha = 1.0F - (tracer.age / (float) LIFETIME);
            Vec3 direction = tracer.to.subtract(tracer.from).normalize();
            consumer.addVertex(matrix, (float) tracer.from.x, (float) tracer.from.y, (float) tracer.from.z)
                    .setColor(1.0F, 0.85F, 0.35F, alpha)
                    .setNormal((float) direction.x, (float) direction.y, (float) direction.z);
            consumer.addVertex(matrix, (float) tracer.to.x, (float) tracer.to.y, (float) tracer.to.z)
                    .setColor(1.0F, 0.45F, 0.1F, alpha * 0.6F)
                    .setNormal((float) direction.x, (float) direction.y, (float) direction.z);
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.lines());
    }

    public static void clear() {
        TRACERS.clear();
    }

    private TracerRenderer() {
    }
}
