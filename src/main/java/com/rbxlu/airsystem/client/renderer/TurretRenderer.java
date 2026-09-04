package com.rbxlu.airsystem.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.rbxlu.airsystem.client.model.ModModelLayers;
import com.rbxlu.airsystem.client.model.TurretModel;
import com.rbxlu.airsystem.content.turret.TurretEntity;
import com.rbxlu.airsystem.content.turret.TurretKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class TurretRenderer extends EntityRenderer<TurretEntity> {
    private final TurretModel model;

    public TurretRenderer(EntityRendererProvider.Context context, TurretKind kind) {
        super(context);
        this.model = new TurretModel(context.bakeLayer(ModModelLayers.turret(kind)));
        this.shadowRadius = kind.getWidth() * 0.45F;
    }

    @Override
    public void render(TurretEntity turret, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        model.setupAnim(turret, 0.0F, 0.0F, turret.tickCount + partialTick, 0.0F, 0.0F);
        VertexConsumer consumer = buffers.getBuffer(model.renderType(getTextureLocation(turret)));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
        super.render(turret, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TurretEntity turret) {
        return turret.getKind().texture();
    }

    @Override
    public boolean shouldShowName(TurretEntity turret) {
        return false;
    }
}
