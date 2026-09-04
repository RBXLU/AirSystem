package com.rbxlu.airsystem.util;

import com.rbxlu.airsystem.content.drone.DroneFlight;
import com.rbxlu.airsystem.content.drone.DroneFlightManager;
import com.rbxlu.airsystem.network.payload.DroneFeedPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DroneFeedSessions {
    private static final class Session {
        private final ResourceKey<Level> dimension;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;
        private final GameType gameType;
        private final UUID flightId;

        private int lastTrackingUpdate;

        private Session(ResourceKey<Level> dimension, double x, double y, double z,
                        float yaw, float pitch, GameType gameType, UUID flightId) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.gameType = gameType;
            this.flightId = flightId;
        }

        private ResourceKey<Level> dimension() {
            return dimension;
        }

        private double x() {
            return x;
        }

        private double y() {
            return y;
        }

        private double z() {
            return z;
        }

        private float yaw() {
            return yaw;
        }

        private float pitch() {
            return pitch;
        }

        private GameType gameType() {
            return gameType;
        }

        private UUID flightId() {
            return flightId;
        }
    }

    // Rebuilding the tracked-entity list walks everything around the player, so it
    // runs on a chunk change plus a rare refresh rather than every tick.
    private static final int TRACKING_INTERVAL = 10;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static boolean isWatching(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void open(ServerPlayer player, DroneFlight flight) {
        if (SESSIONS.containsKey(player.getUUID())) {
            close(player);
        }

        SESSIONS.put(player.getUUID(), new Session(
                player.level().dimension(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                player.gameMode.getGameModeForPlayer(),
                flight.id()));

        player.setGameMode(GameType.SPECTATOR);

        player.connection.teleport(flight.position().x, flight.position().y, flight.position().z,
                0.0F, 0.0F, Set.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT));
        follow(player, flight, SESSIONS.get(player.getUUID()));
        PacketDistributor.sendToPlayer(player, new DroneFeedPayload(flight.netId(), true));
    }

    public static void close(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        restore(player, session);
        PacketDistributor.sendToPlayer(player, new DroneFeedPayload(-1, false));
    }

    private static void restore(ServerPlayer player, Session session) {
        player.setGameMode(session.gameType());
        ServerLevel level = player.server.getLevel(session.dimension());
        if (level != null) {
            player.teleportTo(level, session.x(), session.y(), session.z(), Set.of(),
                    session.yaw(), session.pitch());
        } else {
            player.teleportTo(session.x(), session.y(), session.z());
        }
    }

    private static void follow(ServerPlayer player, DroneFlight flight, Session session) {
        ChunkPos before = player.chunkPosition();
        player.absMoveTo(flight.position().x, flight.position().y, flight.position().z,
                player.getYRot(), player.getXRot());

        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        boolean chunkChanged = !player.chunkPosition().equals(before);
        if (chunkChanged || ++session.lastTrackingUpdate >= TRACKING_INTERVAL) {
            session.lastTrackingUpdate = 0;
            // Moved without a teleport packet: the client already knows where the drone is,
            // while the server needs the position to load chunks around the operator.
            level.getChunkSource().move(player);
        }
    }

    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Session>> iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Session> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            ServerLevel level = player.serverLevel();
            DroneFlight flight = DroneFlightManager.get(level).byId(entry.getValue().flightId());
            if (flight == null || !flight.isAlive()) {
                iterator.remove();
                restore(player, entry.getValue());
                PacketDistributor.sendToPlayer(player, new DroneFeedPayload(-1, false));
                continue;
            }
            follow(player, flight, entry.getValue());
        }
    }

    public static void handleLogout(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            restore(player, session);
        }
    }

    public static BlockPos controlStation(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        return session == null
                ? player.blockPosition()
                : BlockPos.containing(session.x(), session.y(), session.z());
    }

    public static void clear() {
        SESSIONS.clear();
    }

    private DroneFeedSessions() {
    }
}
