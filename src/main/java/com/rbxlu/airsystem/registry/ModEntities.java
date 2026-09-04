package com.rbxlu.airsystem.registry;

import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.content.turret.TurretEntity;
import com.rbxlu.airsystem.content.turret.TurretKind;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, AirSystem.MODID);

    private static final Map<TurretKind, DeferredHolder<EntityType<?>, EntityType<TurretEntity>>> TURRETS =
            new EnumMap<>(TurretKind.class);

    static {
        for (TurretKind kind : TurretKind.values()) {
            String id = "turret_" + kind.getId();
            TURRETS.put(kind, ENTITIES.register(id, () -> EntityType.Builder
                    .<TurretEntity>of((type, level) -> new TurretEntity(type, level, kind), MobCategory.MISC)
                    .sized(kind.getWidth(), kind.getHeight())
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(id)));
        }
    }

    public static DeferredHolder<EntityType<?>, EntityType<TurretEntity>> turretType(TurretKind kind) {
        return TURRETS.get(kind);
    }

    public static Map<TurretKind, DeferredHolder<EntityType<?>, EntityType<TurretEntity>>> turrets() {
        return TURRETS;
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    private ModEntities() {
    }
}
