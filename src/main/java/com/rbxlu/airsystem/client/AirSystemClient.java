package com.rbxlu.airsystem.client;

import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.client.handler.AlarmHud;
import com.rbxlu.airsystem.client.handler.BlastOverlay;
import com.rbxlu.airsystem.client.handler.ImpactVisuals;
import com.rbxlu.airsystem.client.handler.PendingFeed;
import com.rbxlu.airsystem.client.handler.ScreenShakeHandler;
import com.rbxlu.airsystem.client.handler.TracerRenderer;
import com.rbxlu.airsystem.client.model.DroneLayers;
import com.rbxlu.airsystem.client.model.ModModelLayers;
import com.rbxlu.airsystem.client.model.TurretLayers;
import com.rbxlu.airsystem.client.renderer.DroneBlockRenderer;
import com.rbxlu.airsystem.client.renderer.DroneFlightRenderer;
import com.rbxlu.airsystem.client.renderer.TurretRenderer;
import com.rbxlu.airsystem.client.screen.DroneFeedScreen;
import com.rbxlu.airsystem.content.drone.DroneKind;
import com.rbxlu.airsystem.content.turret.TurretEntity;
import com.rbxlu.airsystem.content.turret.TurretKind;
import com.rbxlu.airsystem.network.payload.TurretInputPayload;
import com.rbxlu.airsystem.registry.ModBlockEntities;
import com.rbxlu.airsystem.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = AirSystem.MODID, dist = Dist.CLIENT)
public class AirSystemClient {
    private static boolean lastTurretFiring;

    public AirSystemClient(IEventBus modBus) {
        modBus.addListener(AirSystemClient::registerLayerDefinitions);
        modBus.addListener(AirSystemClient::registerRenderers);
        modBus.addListener(AirSystemClient::registerGuiLayers);
        modBus.addListener(AirSystemClient::registerReloadListeners);

        NeoForge.EVENT_BUS.addListener(AirSystemClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ScreenShakeHandler::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(BlastOverlay::onComputeFov);
        NeoForge.EVENT_BUS.addListener(TracerRenderer::render);
        NeoForge.EVENT_BUS.addListener(DroneFlightRenderer::render);
        NeoForge.EVENT_BUS.addListener(AirSystemClient::onLoggingOut);
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (DroneKind kind : DroneKind.values()) {
            event.registerLayerDefinition(ModModelLayers.drone(kind), () -> DroneLayers.create(kind));
        }
        for (TurretKind kind : TurretKind.values()) {
            event.registerLayerDefinition(ModModelLayers.turret(kind), () -> TurretLayers.create(kind));
        }
        event.registerLayerDefinition(ModModelLayers.AERIAL_BOMB, DroneLayers::createAerialBomb);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (TurretKind kind : TurretKind.values()) {
            event.registerEntityRenderer(ModEntities.turretType(kind).get(),
                    context -> new TurretRenderer(context, kind));
        }
        event.registerBlockEntityRenderer(ModBlockEntities.DRONE.get(), DroneBlockRenderer::new);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "air_raid_alarm"),
                AlarmHud.INSTANCE);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "blast_flash"),
                BlastOverlay.INSTANCE);
    }

    private static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> DroneFlightRenderer.invalidate());
    }

    private static void onClientTick(PlayerTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player) {
            return;
        }

        ScreenShakeHandler.tick();
        ImpactVisuals.tick();
        TracerRenderer.tick();
        AlarmHud.tick();
        BlastOverlay.tick();
        PendingFeed.tick();
        ClientDroneStore.tick();
        tickFeedCamera(minecraft);
        tickTurretControl(minecraft);
    }

    private static void tickFeedCamera(Minecraft minecraft) {
        if (minecraft.player == null || !(minecraft.screen instanceof DroneFeedScreen feed)) {
            return;
        }
        ClientDroneStore.ClientDrone drone = feed.drone();
        if (drone == null) {
            return;
        }
        minecraft.player.setPos(drone.position().x, drone.position().y, drone.position().z);
        minecraft.player.setDeltaMovement(Vec3.ZERO);
    }

    private static void tickTurretControl(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        if (!(minecraft.player.getVehicle() instanceof TurretEntity turret)) {
            lastTurretFiring = false;
            return;
        }

        boolean firing = minecraft.screen == null && minecraft.options.keyAttack.isDown();
        if (firing != lastTurretFiring) {
            lastTurretFiring = firing;
            PacketDistributor.sendToServer(new TurretInputPayload(turret.getId(),
                    minecraft.player.getYRot(), minecraft.player.getXRot(), firing));
        }
    }

    private static void onLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        resetEffects();
    }

    public static void resetEffects() {
        ScreenShakeHandler.clear();
        ImpactVisuals.clear();
        TracerRenderer.clear();
        AlarmHud.clear();
        ClientDroneStore.clear();
        ClientTelemetry.clear();
        PendingFeed.cancel();
    }
}
