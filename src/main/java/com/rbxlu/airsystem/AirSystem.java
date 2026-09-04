package com.rbxlu.airsystem;

import com.rbxlu.airsystem.command.AirSystemCommands;
import com.rbxlu.airsystem.content.drone.DroneFlight;
import com.rbxlu.airsystem.content.drone.DroneFlightManager;
import com.rbxlu.airsystem.network.ModNetwork;
import com.rbxlu.airsystem.registry.ModBlockEntities;
import com.rbxlu.airsystem.registry.ModBlocks;
import com.rbxlu.airsystem.registry.ModCreativeTabs;
import com.rbxlu.airsystem.registry.ModDataComponents;
import com.rbxlu.airsystem.registry.ModEntities;
import com.rbxlu.airsystem.registry.ModItems;
import com.rbxlu.airsystem.registry.ModSounds;
import com.rbxlu.airsystem.util.AirSystemConfig;
import com.rbxlu.airsystem.util.ChunkForcing;
import com.rbxlu.airsystem.util.DroneFeedSessions;
import com.rbxlu.airsystem.util.ImpactEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AirSystem.MODID)
/**
 * Drones are deliberately not entities. They live in DroneFlightManager and sync
 * through their own payloads, so range and server cost do not depend on entity
 * tracking. TACZ support is soft: bullets are recognised by id, never by API.
 */
public class AirSystem {
    public static final String MODID = "airsystem";
    public static final Logger LOGGER = LoggerFactory.getLogger("AirSystem");

    public AirSystem(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, AirSystemConfig.SPEC);

        ModSounds.register(modBus);
        ModDataComponents.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModItems.register(modBus);
        ModEntities.register(modBus);
        ModCreativeTabs.register(modBus);

        modBus.addListener(ModNetwork::register);
        modBus.addListener(ChunkForcing::register);
        modBus.addListener(AirSystem::onConfigLoad);

        NeoForge.EVENT_BUS.addListener(AirSystem::onLevelTick);
        NeoForge.EVENT_BUS.addListener(AirSystem::onServerTick);
        NeoForge.EVENT_BUS.addListener(AirSystem::onServerStopping);
        NeoForge.EVENT_BUS.addListener(AirSystem::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(AirSystem::onExplosion);
        NeoForge.EVENT_BUS.addListener(AirSystemCommands::register);

        LOGGER.info("Project AirSystem loaded: UAVs and air defence ready");
    }

    private static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == AirSystemConfig.SPEC) {
            AirSystemConfig.markLoaded();
        }
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            DroneFlightManager.get(level).tick(level);
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        ImpactEffects.tickShockwaves();
        DroneFeedSessions.tick(event.getServer());
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = event.getExplosion().center();
        double radius = 8.0D;
        AABB box = new AABB(center, center).inflate(radius);
        for (DroneFlight flight : DroneFlightManager.get(level).inBox(box)) {
            flight.hit(level, flight.position(), false, null);
        }
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DroneFeedSessions.handleLogout(player);
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        ImpactEffects.clear();
        DroneFeedSessions.clear();
        ChunkForcing.clear();
    }
}
