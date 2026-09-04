package com.rbxlu.airsystem.registry;

import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.content.alarm.AirRaidSirenBlockEntity;
import com.rbxlu.airsystem.content.alarm.AlarmButtonBlockEntity;
import com.rbxlu.airsystem.content.drone.DroneBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, AirSystem.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AirRaidSirenBlockEntity>> AIR_RAID_SIREN =
            BLOCK_ENTITIES.register("air_raid_siren", () -> BlockEntityType.Builder
                    .of(AirRaidSirenBlockEntity::new, ModBlocks.AIR_RAID_SIREN.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlarmButtonBlockEntity>> ALARM_BUTTON =
            BLOCK_ENTITIES.register("alarm_button", () -> BlockEntityType.Builder
                    .of(AlarmButtonBlockEntity::new, ModBlocks.ALARM_BUTTON.get())
                    .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DroneBlockEntity>> DRONE =
            BLOCK_ENTITIES.register("drone", () -> BlockEntityType.Builder
                    .of(DroneBlockEntity::new, ModBlocks.DRONE.get())
                    .build(null));

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    private ModBlockEntities() {
    }
}
