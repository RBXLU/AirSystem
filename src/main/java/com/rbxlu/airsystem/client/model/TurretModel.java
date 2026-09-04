package com.rbxlu.airsystem.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rbxlu.airsystem.content.turret.TurretEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

public class TurretModel extends EntityModel<TurretEntity> {
    private final ModelPart root;
    private final ModelPart hull;
    @Nullable
    private final ModelPart turret;
    @Nullable
    private final ModelPart barrels;
    @Nullable
    private final ModelPart radar;

    private final float barrelRestZ;

    public TurretModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.hull = root.getChild("hull");
        this.turret = hull.hasChild("turret") ? hull.getChild("turret") : null;
        this.barrels = turret != null && turret.hasChild("barrels") ? turret.getChild("barrels") : null;
        this.radar = turret != null && turret.hasChild("radar") ? turret.getChild("radar") : null;
        this.barrelRestZ = barrels != null ? barrels.z : 0.0F;
    }

    @Override
    public void setupAnim(TurretEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        if (turret != null) {
            turret.yRot = Mth.wrapDegrees(entity.getTurretYaw() - entity.getYRot()) * Mth.DEG_TO_RAD;
        }
        if (barrels != null) {
            barrels.xRot = -entity.getBarrelPitch() * Mth.DEG_TO_RAD;

            barrels.z = barrelRestZ + entity.getRecoil() * 2.0F;
        }
        if (radar != null) {
            radar.yRot = (ageInTicks * 0.07F) % Mth.TWO_PI;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, consumer, packedLight, packedOverlay, color);
    }
}
