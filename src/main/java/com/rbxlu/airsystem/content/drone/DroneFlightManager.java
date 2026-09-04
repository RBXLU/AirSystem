package com.rbxlu.airsystem.content.drone;

import com.rbxlu.airsystem.network.payload.DroneRemovePayload;
import com.rbxlu.airsystem.network.payload.DroneSyncPayload;
import com.rbxlu.airsystem.network.payload.DroneTelemetryPayload;
import com.rbxlu.airsystem.registry.ModDataComponents;
import com.rbxlu.airsystem.util.AirSystemConfig;
import com.rbxlu.airsystem.util.ChunkForcing;
import com.rbxlu.airsystem.util.TaczCompat;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DroneFlightManager extends SavedData {
    private static final String NAME = "airsystem_flights";

    // Every projectile in the mod comes from a player, so with nobody near a drone
    // there is nothing that could have hit it — and the volume query is expensive.
    private static final double PROJECTILE_WATCH = 160.0D;

    // Clients ease over LERP_STEPS ticks, which hides this interval completely.
    private static final int SYNC_INTERVAL = 2;

    private final Map<UUID, DroneFlight> flights = new HashMap<>();
    private int syncCounter;
    private final List<Munition> munitions = new ArrayList<>();
    private int nextNetId = 1;

    public static SavedData.Factory<DroneFlightManager> factory() {
        return new SavedData.Factory<>(DroneFlightManager::new, DroneFlightManager::load, null);
    }

    public static DroneFlightManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), NAME);
    }

    public Collection<DroneFlight> flights() {
        return flights.values();
    }

    @Nullable
    public DroneFlight byId(UUID id) {
        return flights.get(id);
    }

    @Nullable
    public DroneFlight byNetId(int netId) {
        for (DroneFlight flight : flights.values()) {
            if (flight.netId() == netId) {
                return flight;
            }
        }
        return null;
    }

    @Nullable
    public DroneFlight nearestAirborne(Vec3 point, double maxDistance) {
        DroneFlight best = null;
        double bestDistance = maxDistance * maxDistance;
        for (DroneFlight flight : flights.values()) {
            if (!flight.state().isAirborne()) {
                continue;
            }
            double distance = flight.position().distanceToSqr(point);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = flight;
            }
        }
        return best;
    }

    @Nullable
    public DroneFlight nearestHostile(Vec3 point, double maxDistance, double friendlyRadius) {
        DroneFlight best = null;
        double bestDistance = maxDistance * maxDistance;
        for (DroneFlight flight : flights.values()) {
            if (!flight.state().isAirborne() || isFriendly(flight, point, friendlyRadius)) {
                continue;
            }
            double distance = flight.position().distanceToSqr(point);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = flight;
            }
        }
        return best;
    }

    public static boolean isFriendly(DroneFlight flight, Vec3 point, double friendlyRadius) {
        return friendlyRadius > 0.0D && flight.hasHome()
                && flight.home().distToCenterSqr(point) <= friendlyRadius * friendlyRadius;
    }

    public List<DroneFlight> inBox(AABB box) {
        List<DroneFlight> result = new ArrayList<>();
        for (DroneFlight flight : flights.values()) {
            if (box.intersects(flight.box())) {
                result.add(flight);
            }
        }
        return result;
    }

    public DroneFlight launch(ServerLevel level, DroneKind kind, Vec3 position, float yaw,
                              net.minecraft.core.BlockPos target, @Nullable ServerPlayer operator) {
        return launch(level, kind, position, yaw, target, net.minecraft.core.BlockPos.containing(position),
                operator);
    }

    public DroneFlight launch(ServerLevel level, DroneKind kind, Vec3 position, float yaw,
                              net.minecraft.core.BlockPos target, net.minecraft.core.BlockPos home,
                              @Nullable ServerPlayer operator) {
        DroneFlight flight = new DroneFlight(UUID.randomUUID(), nextNetId++, kind, position, yaw);
        flight.setTarget(target);
        flight.setHome(home);
        if (operator != null) {
            flight.setOperator(operator.getUUID());
        }
        flights.put(flight.id(), flight);
        setDirty();
        return flight;
    }

    public void tick(ServerLevel level) {
        if (flights.isEmpty() && munitions.isEmpty()) {
            return;
        }

        List<Integer> removed = null;

        Iterator<DroneFlight> iterator = flights.values().iterator();
        while (iterator.hasNext()) {
            DroneFlight flight = iterator.next();
            ChunkForcing.follow(level, flight.id(), flight.position().x, flight.position().z,
                    flight.velocity().x, flight.velocity().z);
            checkProjectiles(level, flight);

            if (flight.tick(level, this)) {
                ChunkForcing.release(level, flight.id());
                if (flight.state() == DroneState.LANDED) {
                    rebindRemote(level, flight);
                }
                if (removed == null) {
                    removed = new ArrayList<>();
                }
                removed.add(flight.netId());
                iterator.remove();
            }
        }

        Iterator<Munition> munitionIterator = munitions.iterator();
        while (munitionIterator.hasNext()) {
            Munition munition = munitionIterator.next();
            if (munition.tick(level)) {
                if (removed == null) {
                    removed = new ArrayList<>();
                }
                removed.add(munition.netId());
                munitionIterator.remove();
            }
        }

        if (removed != null) {
            broadcast(level, new DroneRemovePayload(removed));
        }
        sync(level);
        setDirty();
    }

    private void rebindRemote(ServerLevel level, DroneFlight flight) {
        if (flight.operator() == null) {
            return;
        }
        ServerPlayer operator = level.getServer().getPlayerList().getPlayer(flight.operator());
        if (operator == null) {
            return;
        }
        net.minecraft.core.BlockPos pad = net.minecraft.core.BlockPos.containing(flight.position());
        for (ItemStack stack : operator.getInventory().items) {
            UUID linked = stack.get(ModDataComponents.LINKED_FLIGHT.get());
            if (linked == null || !linked.equals(flight.id())) {
                continue;
            }
            stack.remove(ModDataComponents.LINKED_FLIGHT.get());
            stack.set(ModDataComponents.LINKED_DRONE.get(), pad);
        }
        operator.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.airsystem.drone.landed", pad.getX(), pad.getY(), pad.getZ())
                .withStyle(net.minecraft.ChatFormatting.GREEN), true);
    }

    public void addMunition(Munition munition) {
        munitions.add(munition);
        setDirty();
    }

    public int nextNetId() {
        return nextNetId++;
    }

    private void checkProjectiles(ServerLevel level, DroneFlight flight) {
        Vec3 position = flight.position();
        if (!level.hasNearbyAlivePlayer(position.x, position.y, position.z, PROJECTILE_WATCH)) {
            return;
        }

        AABB box = flight.box();
        List<Entity> candidates = level.getEntities((Entity) null, box.inflate(6.0D),
                entity -> entity.isAlive() && isProjectile(entity));
        if (candidates.isEmpty()) {
            return;
        }

        for (Entity projectile : candidates) {
            Vec3 to = projectile.position();
            Vec3 from = to.subtract(projectile.getDeltaMovement());
            var hit = box.clip(from, to);
            if (hit.isEmpty()) {
                continue;
            }

            Entity shooter = projectile instanceof Projectile arrow ? arrow.getOwner() : null;
            if (flight.hit(level, hit.get(), true, shooter)) {
                projectile.discard();
            }
        }
    }

    private static boolean isProjectile(Entity entity) {
        return entity instanceof Projectile || TaczCompat.isGunProjectile(entity);
    }

    private void sync(ServerLevel level) {
        if (level.players().isEmpty()) {
            return;
        }
        if (++syncCounter % SYNC_INTERVAL != 0) {
            return;
        }

        double range = AirSystemConfig.syncRange();
        double rangeSqr = range * range;

        List<DroneFlight> ordered = new ArrayList<>(flights.values());
        List<DroneSyncPayload.Snapshot> allDrones = new ArrayList<>(ordered.size());
        for (DroneFlight flight : ordered) {
            allDrones.add(DroneSyncPayload.Snapshot.of(flight));
        }
        List<DroneSyncPayload.MunitionSnapshot> allBombs = new ArrayList<>(munitions.size());
        for (Munition munition : munitions) {
            allBombs.add(DroneSyncPayload.MunitionSnapshot.of(munition));
        }

        for (ServerPlayer player : level.players()) {
            Vec3 playerPosition = player.position();
            UUID playerId = player.getUUID();

            List<DroneSyncPayload.Snapshot> visible = new ArrayList<>(ordered.size());
            for (int i = 0; i < ordered.size(); i++) {
                DroneFlight flight = ordered.get(i);
                boolean operating = playerId.equals(flight.operator());
                if (operating || playerPosition.distanceToSqr(flight.position()) <= rangeSqr) {
                    visible.add(allDrones.get(i));
                }
            }

            List<DroneSyncPayload.MunitionSnapshot> bombs = new ArrayList<>(munitions.size());
            for (int i = 0; i < munitions.size(); i++) {
                if (playerPosition.distanceToSqr(munitions.get(i).position()) <= rangeSqr) {
                    bombs.add(allBombs.get(i));
                }
            }

            if (!visible.isEmpty() || !bombs.isEmpty()) {
                PacketDistributor.sendToPlayer(player, new DroneSyncPayload(visible, bombs));
            }
            sendTelemetry(player);
        }
    }

    private void sendTelemetry(ServerPlayer player) {
        if (player.tickCount % 5 != 0) {
            return;
        }
        for (DroneFlight flight : flights.values()) {
            if (flight.operator() == null || !flight.operator().equals(player.getUUID())) {
                continue;
            }
            PacketDistributor.sendToPlayer(player, new DroneTelemetryPayload(
                    flight.netId(), flight.target(), flight.fuel(), flight.munitions(),
                    flight.kind().getCoreHits() - flight.coreHits()));
        }
    }

    private void broadcast(ServerLevel level, DroneRemovePayload payload) {
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public int abortAll(ServerLevel level) {
        int count = flights.size();
        List<Integer> removed = new ArrayList<>();
        for (DroneFlight flight : flights.values()) {
            ChunkForcing.release(level, flight.id());
            removed.add(flight.netId());
        }
        flights.clear();
        for (Munition munition : munitions) {
            removed.add(munition.netId());
        }
        munitions.clear();
        if (!removed.isEmpty()) {
            broadcast(level, new DroneRemovePayload(removed));
        }
        setDirty();
        return count;
    }

    public void remove(ServerLevel level, DroneFlight flight) {
        if (flights.remove(flight.id()) != null) {
            ChunkForcing.release(level, flight.id());
            broadcast(level, new DroneRemovePayload(List.of(flight.netId())));
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (DroneFlight flight : flights.values()) {
            list.add(flight.save());
        }
        tag.put("Flights", list);

        ListTag bombs = new ListTag();
        for (Munition munition : munitions) {
            bombs.add(munition.save());
        }
        tag.put("Munitions", bombs);
        tag.putInt("NextNetId", nextNetId);
        return tag;
    }

    private static DroneFlightManager load(CompoundTag tag, HolderLookup.Provider registries) {
        DroneFlightManager manager = new DroneFlightManager();
        manager.nextNetId = Math.max(1, tag.getInt("NextNetId"));

        ListTag list = tag.getList("Flights", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            DroneFlight flight = DroneFlight.load(list.getCompound(i), manager.nextNetId++);
            manager.flights.put(flight.id(), flight);
        }

        ListTag bombs = tag.getList("Munitions", Tag.TAG_COMPOUND);
        for (int i = 0; i < bombs.size(); i++) {
            manager.munitions.add(Munition.load(bombs.getCompound(i), manager.nextNetId++));
        }
        return manager;
    }
}
