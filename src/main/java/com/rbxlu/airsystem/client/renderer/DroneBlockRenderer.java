package com.rbxlu.airsystem.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rbxlu.airsystem.client.model.DroneModel;
import com.rbxlu.airsystem.client.model.ModModelLayers;
import com.rbxlu.airsystem.content.drone.DroneBlock;
import com.rbxlu.airsystem.content.drone.DroneBlockEntity;
import com.rbxlu.airsystem.content.drone.DroneKind;
import com.rbxlu.airsystem.content.drone.DroneState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.EnumMap;
import java.util.Map;

public class DroneBlockRenderer implements BlockEntityRenderer<DroneBlockEntity> {
    private final BlockEntityRendererProvider.Context context;
    private final Map<DroneKind, DroneModel> models = new EnumMap<>(DroneKind.class);

    public DroneBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    private DroneModel model(DroneKind kind) {
        return models.computeIfAbsent(kind,
                key -> new DroneModel(context.bakeLayer(ModModelLayers.drone(key))));
    }

    @Override
    public void render(DroneBlockEntity drone, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.05D, 0.5D);

        float yaw = drone.getBlockState().getValue(DroneBlock.FACING).toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        DroneModel model = model(drone.getKind());
        model.setup(DroneState.IDLE, false, 0.0F, 0.0F);
        model.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(drone.getKind().texture())),
                packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen(DroneBlockEntity drone) {
        return true;
    }
}
