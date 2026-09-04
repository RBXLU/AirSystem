package com.rbxlu.airsystem.client;

import com.rbxlu.airsystem.network.payload.DroneTelemetryPayload;
import net.minecraft.core.BlockPos;

public final class ClientTelemetry {
    private static int netId = -1;
    private static BlockPos target = BlockPos.ZERO;
    private static int fuel;
    private static int munitions;
    private static int hitsLeft;

    public static void accept(DroneTelemetryPayload payload) {
        netId = payload.netId();
        target = payload.target();
        fuel = payload.fuel();
        munitions = payload.munitions();
        hitsLeft = payload.hitsLeft();
    }

    public static boolean isFor(int id) {
        return netId == id;
    }

    public static BlockPos target() {
        return target;
    }

    public static boolean hasTarget() {
        return !BlockPos.ZERO.equals(target);
    }

    public static int fuel() {
        return fuel;
    }

    public static int munitions() {
        return munitions;
    }

    public static int hitsLeft() {
        return hitsLeft;
    }

    public static void clear() {
        netId = -1;
        target = BlockPos.ZERO;
        fuel = 0;
        munitions = 0;
        hitsLeft = 0;
    }

    private ClientTelemetry() {
    }
}
