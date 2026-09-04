package com.rbxlu.airsystem.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.client.model.ModModelLayers;
import com.rbxlu.airsystem.content.radar.RadarBlock;
import com.rbxlu.airsystem.content.radar.RadarBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RadarRenderer implements BlockEntityRenderer<RadarBlockEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "textures/entity/radar.png");

    private static final float DEGREES_PER_TICK = 3.0F;

    private final ModelPart root;
    private final ModelPart dish;

    public RadarRenderer(BlockEntityRendererProvider.Context context) {
        this.root = context.bakeLayer(ModModelLayers.RADAR);
        this.dish = root.getChild("dish");
    }

    @Override
    public void render(RadarBlockEntity radar, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (radar.getLevel() == null) {
            return;
        }
        float time = radar.getLevel().getGameTime() + partialTick;
        dish.yRot = (time * DEGREES_PER_TICK % 360.0F) * Mth.DEG_TO_RAD;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.02D, 0.5D);
        float yaw = radar.getBlockState().getValue(RadarBlock.FACING).toYRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        root.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
