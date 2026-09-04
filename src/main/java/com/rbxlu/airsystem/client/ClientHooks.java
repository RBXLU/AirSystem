package com.rbxlu.airsystem.client;

import com.rbxlu.airsystem.client.screen.RemoteControlScreen;
import com.rbxlu.airsystem.client.screen.WorldMapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

public final class ClientHooks {
    public static void openRemoteControl(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new RemoteControlScreen(hand));
    }

    public static void openWorldMap(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new WorldMapScreen(hand));
    }

    private ClientHooks() {
    }
}
