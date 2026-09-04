package com.rbxlu.airsystem.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public class AerialBombModel {
    private final ModelPart root;
    private final ModelPart body;

    public AerialBombModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
    }

    public void setup(float ageInTicks) {
        body.zRot = Mth.sin(ageInTicks * 0.35F) * 0.08F;
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                       int packedOverlay, int color) {
        root.render(poseStack, consumer, packedLight, packedOverlay, color);
    }
}
