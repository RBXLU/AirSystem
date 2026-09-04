package com.rbxlu.airsystem.network;

import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.network.payload.AlarmPayload;
import com.rbxlu.airsystem.network.payload.DroneCommandPayload;
import com.rbxlu.airsystem.network.payload.DroneFeedPayload;
import com.rbxlu.airsystem.network.payload.DroneInputPayload;
import com.rbxlu.airsystem.network.payload.DroneRemovePayload;
import com.rbxlu.airsystem.network.payload.DroneSyncPayload;
import com.rbxlu.airsystem.network.payload.DroneTelemetryPayload;
import com.rbxlu.airsystem.network.payload.ImpactPayload;
import com.rbxlu.airsystem.network.payload.RadarContactsPayload;
import com.rbxlu.airsystem.network.payload.RadarQueryPayload;
import com.rbxlu.airsystem.network.payload.RemoteActionPayload;
import com.rbxlu.airsystem.network.payload.TracerPayload;
import com.rbxlu.airsystem.network.payload.TurretInputPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private static final String VERSION = "1.0";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(AirSystem.MODID).versioned(VERSION);

        registrar.playToClient(ImpactPayload.TYPE, ImpactPayload.STREAM_CODEC,
                (payload, context) -> com.rbxlu.airsystem.client.ClientPayloadHandler.handleImpact(payload, context));
        registrar.playToClient(DroneFeedPayload.TYPE, DroneFeedPayload.STREAM_CODEC,
                (payload, context) -> com.rbxlu.airsystem.client.ClientPayloadHandler.handleFeed(payload, context));
        registrar.playToClient(AlarmPayload.TYPE, AlarmPayload.STREAM_CODEC,
                (payload, context) -> com.rbxlu.airsystem.client.ClientPayloadHandler.handleAlarm(payload, context));
        registrar.playToClient(DroneSyncPayload.TYPE, DroneSyncPayload.STREAM_CODEC,
                (payload, context) -> com.rbxlu.airsystem.client.ClientPayloadHandler.handleSync(payload, context));
        registrar.playToClient(DroneRemovePayload.TYPE, DroneRemovePayload.STREAM_CODEC,
                (payload, context) -> com.rbxlu.airsystem.client.ClientPayloadHandler.handleRemove(payload, context));
        registrar.playToClient(DroneTelemetryPayload.TYPE, DroneTelemetryPayload.STREAM_CODEC,
                (payload, context) -> com.rbxlu.airsystem.client.ClientPayloadHandler.handleTelemetry(payload, context));
        registrar.playToClient(TracerPayload.TYPE, TracerPayload.STREAM_CODEC,
                (payload, context) -> com.rbxlu.airsystem.client.ClientPayloadHandler.handleTracer(payload, context));

        registrar.playToClient(RadarContactsPayload.TYPE, RadarContactsPayload.STREAM_CODEC,
                (payload, context) -> com.rbxlu.airsystem.client.ClientPayloadHandler.handleRadar(payload, context));

        registrar.playToServer(RadarQueryPayload.TYPE, RadarQueryPayload.STREAM_CODEC,
                ServerPayloadHandler::handleRadarQuery);
        registrar.playToServer(DroneInputPayload.TYPE, DroneInputPayload.STREAM_CODEC,
                ServerPayloadHandler::handleDroneInput);
        registrar.playToServer(DroneCommandPayload.TYPE, DroneCommandPayload.STREAM_CODEC,
                ServerPayloadHandler::handleDroneCommand);
        registrar.playToServer(RemoteActionPayload.TYPE, RemoteActionPayload.STREAM_CODEC,
                ServerPayloadHandler::handleRemoteAction);
        registrar.playToServer(TurretInputPayload.TYPE, TurretInputPayload.STREAM_CODEC,
                ServerPayloadHandler::handleTurretInput);
    }

    private ModNetwork() {
    }
}
