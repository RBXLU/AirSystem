package com.rbxlu.airsystem.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rbxlu.airsystem.content.drone.DroneState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

public class DroneModel {
    private final ModelPart root;
    private final ModelPart body;
    @Nullable
    private final ModelPart propeller;
    @Nullable
    private final ModelPart rotor;

    public DroneModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.propeller = body.hasChild("propeller") ? body.getChild("propeller") : null;
        this.rotor = body.hasChild("rotor") ? body.getChild("rotor") : null;
    }

    public void setup(DroneState state, boolean engineDead, float spinDegrees, float ageInTicks) {
        float spin = spinDegrees * Mth.DEG_TO_RAD;
        if (propeller != null) {
            propeller.zRot = spin;
        }
        if (rotor != null) {
            rotor.zRot = spin;
        }

        if (state.isAirborne()) {
            float amplitude = engineDead ? 0.09F : 0.018F;
            body.zRot = Mth.sin(ageInTicks * 0.13F) * amplitude;
            body.xRot = Mth.cos(ageInTicks * 0.09F) * amplitude * 0.6F;
        } else {
            body.zRot = 0.0F;
            body.xRot = 0.0F;
        }
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                       int packedOverlay, int color) {
        root.render(poseStack, consumer, packedLight, packedOverlay, color);
    }
}
