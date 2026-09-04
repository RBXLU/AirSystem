package com.rbxlu.airsystem.client;

import com.rbxlu.airsystem.client.handler.AlarmHud;
import com.rbxlu.airsystem.client.handler.ImpactVisuals;
import com.rbxlu.airsystem.client.handler.PendingFeed;
import com.rbxlu.airsystem.client.handler.TracerRenderer;
import com.rbxlu.airsystem.client.screen.DroneFeedScreen;
import com.rbxlu.airsystem.network.payload.AlarmPayload;
import com.rbxlu.airsystem.network.payload.DroneFeedPayload;
import com.rbxlu.airsystem.network.payload.DroneRemovePayload;
import com.rbxlu.airsystem.network.payload.DroneSyncPayload;
import com.rbxlu.airsystem.network.payload.DroneTelemetryPayload;
import com.rbxlu.airsystem.network.payload.ImpactPayload;
import com.rbxlu.airsystem.network.payload.TracerPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPayloadHandler {
    public static void handleImpact(ImpactPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ImpactVisuals.spawn(
                new Vec3(payload.x(), payload.y(), payload.z()),
                payload.power(), payload.incendiary(), payload.wreck()));
    }

    public static void handleSync(DroneSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientDroneStore.accept(payload));
    }

    public static void handleRemove(DroneRemovePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientDroneStore.remove(payload.netIds()));
    }

    public static void handleTelemetry(DroneTelemetryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientTelemetry.accept(payload));
    }

    public static void handleFeed(DroneFeedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (!payload.open()) {
                PendingFeed.cancel();
                ClientTelemetry.clear();
                if (minecraft.screen instanceof DroneFeedScreen) {
                    minecraft.setScreen(null);
                }
                return;
            }
            PendingFeed.request(payload.entityId());
        });
    }

    public static void handleAlarm(AlarmPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> AlarmHud.trigger(payload.mode() == AlarmPayload.MODE_ALERT));
    }

    public static void handleTracer(TracerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TracerRenderer.add(
                new Vec3(payload.fromX(), payload.fromY(), payload.fromZ()),
                new Vec3(payload.toX(), payload.toY(), payload.toZ())));
    }

    private ClientPayloadHandler() {
    }
}
