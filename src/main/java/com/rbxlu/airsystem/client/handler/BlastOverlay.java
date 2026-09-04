package com.rbxlu.airsystem.client.handler;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

public final class BlastOverlay implements LayeredDraw.Layer {
    public static final BlastOverlay INSTANCE = new BlastOverlay();

    private static float intensity;
    private static float previousIntensity;
    private static float punch;
    private static float previousPunch;

    public static void flash(float value) {
        intensity = Math.min(1.0F, Math.max(intensity, value));
        punch = Math.min(0.22F, Math.max(punch, value * 0.18F));
    }

    public static void tick() {
        previousIntensity = intensity;
        previousPunch = punch;

        intensity *= intensity > 0.35F ? 0.55F : 0.80F;
        punch *= 0.72F;
        if (intensity < 0.004F) {
            intensity = 0.0F;
        }
        if (punch < 0.002F) {
            punch = 0.0F;
        }
    }

    public static void clear() {
        intensity = 0.0F;
        previousIntensity = 0.0F;
        punch = 0.0F;
        previousPunch = 0.0F;
    }

    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (punch <= 0.0F && previousPunch <= 0.0F) {
            return;
        }
        float value = Mth.lerp(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false),
                previousPunch, punch);
        event.setNewFovModifier(event.getNewFovModifier() * (1.0F + value));
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        float value = Mth.lerp(delta.getGameTimeDeltaPartialTick(false), previousIntensity, intensity);
        if (value <= 0.004F) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return;
        }

        float warm = Mth.clamp(1.0F - value, 0.0F, 1.0F);
        int red = 255;
        int green = (int) (216 + 39 * value - 60 * warm * value);
        int blue = (int) (150 + 105 * value - 130 * warm * value);
        int alpha = (int) (Mth.clamp(value, 0.0F, 1.0F) * 235);

        int colour = (alpha << 24) | (red << 16) | (Mth.clamp(green, 0, 255) << 8) | Mth.clamp(blue, 0, 255);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), colour);
    }

    private BlastOverlay() {
    }
}
