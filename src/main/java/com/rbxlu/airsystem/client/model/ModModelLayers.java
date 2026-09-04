package com.rbxlu.airsystem.client.model;

import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.content.drone.DroneKind;
import com.rbxlu.airsystem.content.turret.TurretKind;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public final class ModModelLayers {
    private static final Map<DroneKind, ModelLayerLocation> DRONES = new EnumMap<>(DroneKind.class);
    private static final Map<TurretKind, ModelLayerLocation> TURRETS = new EnumMap<>(TurretKind.class);

    public static final ModelLayerLocation AERIAL_BOMB = create("aerial_bomb");

    static {
        for (DroneKind kind : DroneKind.values()) {
            DRONES.put(kind, create("drone_" + kind.getId()));
        }
        for (TurretKind kind : TurretKind.values()) {
            TURRETS.put(kind, create("turret_" + kind.getId()));
        }
    }

    private static ModelLayerLocation create(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, name), "main");
    }

    public static ModelLayerLocation drone(DroneKind kind) {
        return DRONES.get(kind);
    }

    public static ModelLayerLocation turret(TurretKind kind) {
        return TURRETS.get(kind);
    }

    private ModModelLayers() {
    }
}
