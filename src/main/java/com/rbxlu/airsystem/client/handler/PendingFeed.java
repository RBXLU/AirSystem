package com.rbxlu.airsystem.client.handler;

import com.rbxlu.airsystem.client.ClientDroneStore;
import com.rbxlu.airsystem.client.screen.DroneFeedScreen;
import net.minecraft.client.Minecraft;

public final class PendingFeed {
    private static final int TIMEOUT = 100;

    private static int netId = -1;
    private static int waitedTicks;

    public static void request(int id) {
        netId = id;
        waitedTicks = 0;
        tryOpen();
    }

    public static void cancel() {
        netId = -1;
        waitedTicks = 0;
    }

    public static void tick() {
        if (netId < 0) {
            return;
        }
        if (++waitedTicks > TIMEOUT) {
            cancel();
            return;
        }
        tryOpen();
    }

    private static void tryOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ClientDroneStore.byNetId(netId) == null) {
            return;
        }
        int id = netId;
        netId = -1;
        waitedTicks = 0;
        if (!(minecraft.screen instanceof DroneFeedScreen)) {
            minecraft.setScreen(new DroneFeedScreen(id));
        }
    }

    private PendingFeed() {
    }
}
