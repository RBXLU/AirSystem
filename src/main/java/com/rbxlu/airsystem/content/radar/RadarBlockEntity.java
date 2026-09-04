package com.rbxlu.airsystem.content.radar;

import com.rbxlu.airsystem.content.drone.DroneFlight;
import com.rbxlu.airsystem.content.drone.DroneFlightManager;
import com.rbxlu.airsystem.registry.ModBlockEntities;
import com.rbxlu.airsystem.util.AirSystemConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RadarBlockEntity extends BlockEntity {
    private List<RadarContact> contacts = List.of();
    private int sweepCooldown;

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR.get(), pos, state);
    }

    public List<RadarContact> contacts() {
        return Collections.unmodifiableList(contacts);
    }

    /** Height of the dish above the block, where the beam is taken from. */
    private Vec3 antenna() {
        return Vec3.atCenterOf(worldPosition).add(0.0D, 2.0D, 0.0D);
    }

    public void serverTick() {
        if (--sweepCooldown > 0) {
            return;
        }
        sweepCooldown = AirSystemConfig.radarSweepTicks();

        ServerLevel level = (ServerLevel) this.level;
        DroneFlightManager manager = DroneFlightManager.get(level);
        double range = AirSystemConfig.radarRange();
        double friendlyRadius = AirSystemConfig.friendlyRadius();
        Vec3 origin = antenna();

        List<RadarContact> found = new ArrayList<>();
        for (DroneFlight flight : manager.flights()) {
            if (!flight.state().isAirborne()) {
                continue;
            }
            Vec3 position = flight.position();
            if (position.distanceToSqr(origin) > range * range || !visible(level, origin, position)) {
                continue;
            }
            found.add(new RadarContact(flight.netId(),
                    (float) position.x, (float) position.y, (float) position.z,
                    flight.yaw(), (float) flight.velocity().length(),
                    flight.kind().ordinal(),
                    DroneFlightManager.isFriendly(flight, origin, friendlyRadius)));
        }
        contacts = found;
    }

    /**
     * Terrain masks the beam: a drone behind a ridge is not on the scope until it
     * clears the skyline, which is what makes low approaches worth flying.
     */
    private static boolean visible(ServerLevel level, Vec3 from, Vec3 to) {
        BlockHitResult hit = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS;
    }
}
