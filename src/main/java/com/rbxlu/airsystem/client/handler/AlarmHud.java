package com.rbxlu.airsystem.client.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class AlarmHud implements LayeredDraw.Layer {
    public static final AlarmHud INSTANCE = new AlarmHud();

    private static final int BANNER_TICKS = 180;

    private static int alertTicks;
    private static boolean allClear;

    public static void trigger(boolean alert) {
        alertTicks = BANNER_TICKS;
        allClear = !alert;
    }

    public static void tick() {
        if (alertTicks > 0) {
            alertTicks--;
        }
    }

    public static boolean isAlarmActive() {
        return alertTicks > 0 && !allClear;
    }

    public static void clear() {
        alertTicks = 0;
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        if (alertTicks <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return;
        }

        int width = graphics.guiWidth();
        Font font = minecraft.font;

        float pulse = (Mth.sin((minecraft.player != null ? minecraft.player.tickCount : 0) * 0.25F) + 1.0F) * 0.5F;
        int color = allClear
                ? 0xFF3CB371
                : (0xFF000000 | ((int) (200 + 55 * pulse) << 16) | 0x1010);

        Component title = Component.translatable(allClear
                ? "hud.airsystem.all_clear"
                : "hud.airsystem.air_raid");
        Component subtitle = Component.translatable(allClear
                ? "hud.airsystem.all_clear.sub"
                : "hud.airsystem.air_raid.sub");

        int bannerHeight = 34;
        int alpha = (int) (110 + 60 * pulse) << 24;
        graphics.fill(0, 18, width, 18 + bannerHeight, alpha | (allClear ? 0x104010 : 0x400000));

        graphics.drawCenteredString(font, title.copy().withStyle(ChatFormatting.BOLD), width / 2, 24, color);
        graphics.drawCenteredString(font, subtitle, width / 2, 38, 0xFFCCCCCC);

        if (!allClear) {
            int edge = (int) (0x30 + 0x20 * pulse) << 24;
            graphics.fill(0, 0, width, 4, edge | 0xAA0000);
            graphics.fill(0, graphics.guiHeight() - 4, width, graphics.guiHeight(), edge | 0xAA0000);
        }
    }

    private AlarmHud() {
    }
}
