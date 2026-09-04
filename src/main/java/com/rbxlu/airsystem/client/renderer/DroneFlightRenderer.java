package com.rbxlu.airsystem.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.client.ClientDroneStore;
import com.rbxlu.airsystem.client.model.AerialBombModel;
import com.rbxlu.airsystem.client.model.DroneModel;
import com.rbxlu.airsystem.client.model.ModModelLayers;
import com.rbxlu.airsystem.content.drone.DroneKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.EnumMap;
import java.util.Map;

public final class DroneFlightRenderer {
    private static final ResourceLocation BOMB_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "textures/entity/drone/aerial_bomb.png");

    private static final Map<DroneKind, DroneModel> MODELS = new EnumMap<>(DroneKind.class);
    private static AerialBombModel bombModel;

    public static void invalidate() {
        MODELS.clear();
        bombModel = null;
    }

    private static DroneModel model(DroneKind kind) {
        return MODELS.computeIfAbsent(kind, key -> new DroneModel(
                Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.drone(key))));
    }

    private static AerialBombModel bombModel() {
        if (bombModel == null) {
            bombModel = new AerialBombModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.AERIAL_BOMB));
        }
        return bombModel;
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (ClientDroneStore.drones().isEmpty() && ClientDroneStore.munitions().isEmpty()) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        var frustum = event.getFrustum();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        BlockPos.MutableBlockPos lightPos = new BlockPos.MutableBlockPos();

        for (ClientDroneStore.ClientDrone drone : ClientDroneStore.drones()) {
            Vec3 position = drone.lerpPosition(partialTick);

            double reach = Math.max(drone.kind().getLength(), drone.kind().getWingspan());
            if (!frustum.isVisible(new AABB(position.x - reach, position.y - reach, position.z - reach,
                    position.x + reach, position.y + reach, position.z + reach))) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - drone.lerpYaw(partialTick)));
            poseStack.mulPose(Axis.XP.rotationDegrees(-drone.lerpPitch(partialTick)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(drone.lerpRoll(partialTick)));

            // Vanilla models are authored upside down.
            poseStack.scale(-1.0F, -1.0F, 1.0F);

            DroneModel model = model(drone.kind());
            model.setup(drone.state(), drone.engineDead(), drone.propellerSpin(),
                    drone.age() + partialTick);

            lightPos.set(Mth.floor(position.x), Mth.floor(position.y), Mth.floor(position.z));
            int light = LevelRenderer.getLightColor(minecraft.level, lightPos);
            VertexConsumer consumer = buffers.getBuffer(
                    RenderType.entityCutoutNoCull(drone.kind().texture()));
            model.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            poseStack.popPose();
        }

        for (ClientDroneStore.ClientMunition munition : ClientDroneStore.munitions()) {
            Vec3 position = munition.lerpPosition(partialTick);
            if (!frustum.isVisible(new AABB(position.x - 2.0D, position.y - 2.0D, position.z - 2.0D,
                    position.x + 2.0D, position.y + 2.0D, position.z + 2.0D))) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - munition.yaw()));
            poseStack.mulPose(Axis.XP.rotationDegrees(-munition.pitch()));
            poseStack.scale(-1.0F, -1.0F, 1.0F);

            AerialBombModel model = bombModel();
            model.setup(munition.age() + partialTick);
            lightPos.set(Mth.floor(position.x), Mth.floor(position.y), Mth.floor(position.z));
            int light = LevelRenderer.getLightColor(minecraft.level, lightPos);
            model.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(BOMB_TEXTURE)),
                    light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            poseStack.popPose();
        }

        buffers.endBatch();
    }

    private DroneFlightRenderer() {
    }
}
