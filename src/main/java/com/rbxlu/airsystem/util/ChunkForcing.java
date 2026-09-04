package com.rbxlu.airsystem.util;

import com.rbxlu.airsystem.AirSystem;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A drone flies kilometres from any player, so the chunks under it have to be
 * force-loaded or it stalls at the edge of the loaded area.
 *
 * <p>The held area is recomputed only when the drone changes chunk, and is biased
 * along the course: the server has to tick every held chunk in full, and the drone
 * is not coming back. Recomputing it every tick cost more than the whole of the
 * rest of the flight logic.</p>
 */
public final class ChunkForcing {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "drone_flight");
    public static final TicketController CONTROLLER = new TicketController(ID);

    private static final class Held {
        private final LongOpenHashSet chunks = new LongOpenHashSet();
        private long center = Long.MIN_VALUE;
        private int radius = -1;
    }

    private static final Map<UUID, Held> FORCED = new HashMap<>();

    public static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    public static void follow(ServerLevel level, UUID owner, double x, double z,
                              double dx, double dz) {
        int radius = AirSystemConfig.chunkLoadRadius();
        long center = ChunkPos.asLong(Mth.floor(x) >> 4, Mth.floor(z) >> 4);

        Held held = FORCED.computeIfAbsent(owner, key -> new Held());
        // Same chunk: the held area is unchanged.
        if (held.center == center && held.radius == radius) {
            return;
        }
        held.center = center;
        held.radius = radius;

        int signX = dx > 0.05D ? 1 : dx < -0.05D ? -1 : 0;
        int signZ = dz > 0.05D ? 1 : dz < -0.05D ? -1 : 0;
        int backward = -1;

        int side = 2 * radius + 1;
        LongOpenHashSet wanted = new LongOpenHashSet(side * side);
        int cx = ChunkPos.getX(center);
        int cz = ChunkPos.getZ(center);
        for (int ox = -radius; ox <= radius; ox++) {
            if (signX != 0 && ox * signX < backward) {
                continue;
            }
            for (int oz = -radius; oz <= radius; oz++) {
                if (signZ != 0 && oz * signZ < backward) {
                    continue;
                }
                wanted.add(ChunkPos.asLong(cx + ox, cz + oz));
            }
        }

        for (LongIterator it = wanted.iterator(); it.hasNext(); ) {
            long chunk = it.nextLong();
            if (held.chunks.add(chunk)) {
                CONTROLLER.forceChunk(level, owner, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true, true);
            }
        }
        for (LongIterator it = held.chunks.iterator(); it.hasNext(); ) {
            long chunk = it.nextLong();
            if (!wanted.contains(chunk)) {
                CONTROLLER.forceChunk(level, owner, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
                it.remove();
            }
        }
    }

    public static void release(ServerLevel level, UUID owner) {
        Held held = FORCED.remove(owner);
        if (held == null) {
            return;
        }
        for (LongIterator it = held.chunks.iterator(); it.hasNext(); ) {
            long chunk = it.nextLong();
            CONTROLLER.forceChunk(level, owner, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
        }
    }

    public static int forcedChunkCount() {
        int total = 0;
        for (Held held : FORCED.values()) {
            total += held.chunks.size();
        }
        return total;
    }

    public static void clear() {
        FORCED.clear();
    }

    private ChunkForcing() {
    }
}
