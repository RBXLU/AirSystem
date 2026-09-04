package com.rbxlu.airsystem.client;

import com.rbxlu.airsystem.client.screen.RadarScreen;
import com.rbxlu.airsystem.client.screen.RemoteControlScreen;
import com.rbxlu.airsystem.client.screen.WorldMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;

public final class ClientHooks {
    public static void openRemoteControl(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new RemoteControlScreen(hand));
    }

    public static void openWorldMap(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new WorldMapScreen(hand));
    }

    public static void openRadarScreen(BlockPos screen) {
        Minecraft.getInstance().setScreen(new RadarScreen(screen));
    }

    private ClientHooks() {
    }
}
