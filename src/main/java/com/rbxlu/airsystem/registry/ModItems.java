package com.rbxlu.airsystem.registry;

import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.content.drone.DroneKind;
import com.rbxlu.airsystem.content.item.DroneItem;
import com.rbxlu.airsystem.content.item.LinkingCableItem;
import com.rbxlu.airsystem.content.item.RemoteControlItem;
import com.rbxlu.airsystem.content.item.TurretItem;
import com.rbxlu.airsystem.content.item.WorldMapItem;
import com.rbxlu.airsystem.content.turret.TurretKind;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, AirSystem.MODID);

    private static final Map<DroneKind, DeferredHolder<Item, DroneItem>> DRONE_ITEMS =
            new EnumMap<>(DroneKind.class);
    private static final Map<TurretKind, DeferredHolder<Item, TurretItem>> TURRET_ITEMS =
            new EnumMap<>(TurretKind.class);

    public static final DeferredHolder<Item, WorldMapItem> WORLD_MAP = ITEMS.register("world_map",
            () -> new WorldMapItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, RemoteControlItem> REMOTE_CONTROL = ITEMS.register("remote_control",
            () -> new RemoteControlItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, LinkingCableItem> LINKING_CABLE = ITEMS.register("linking_cable",
            () -> new LinkingCableItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, Item> AMMO_35MM = ITEMS.register("ammo_35mm",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> AMMO_30MM = ITEMS.register("ammo_30mm",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> DRONE_FRAME = ITEMS.register("drone_frame",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENGINE_MODULE = ITEMS.register("engine_module",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> WARHEAD_MODULE = ITEMS.register("warhead_module",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CAMERA_MODULE = ITEMS.register("camera_module",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> GUIDANCE_MODULE = ITEMS.register("guidance_module",
            () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, BlockItem> AIR_RAID_SIREN = ITEMS.register("air_raid_siren",
            () -> new BlockItem(ModBlocks.AIR_RAID_SIREN.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> ALARM_BUTTON = ITEMS.register("alarm_button",
            () -> new BlockItem(ModBlocks.ALARM_BUTTON.get(), new Item.Properties()));

    static {
        for (DroneKind kind : DroneKind.values()) {
            DRONE_ITEMS.put(kind, ITEMS.register(kind.getId(),
                    () -> new DroneItem(kind, new Item.Properties().stacksTo(1))));
        }
        for (TurretKind kind : TurretKind.values()) {
            TURRET_ITEMS.put(kind, ITEMS.register(kind.getId(),
                    () -> new TurretItem(kind, new Item.Properties().stacksTo(1))));
        }
    }

    public static DeferredHolder<Item, DroneItem> droneItem(DroneKind kind) {
        return DRONE_ITEMS.get(kind);
    }

    public static DeferredHolder<Item, TurretItem> turretItem(TurretKind kind) {
        return TURRET_ITEMS.get(kind);
    }

    public static Map<DroneKind, DeferredHolder<Item, DroneItem>> droneItems() {
        return DRONE_ITEMS;
    }

    public static Map<TurretKind, DeferredHolder<Item, TurretItem>> turretItems() {
        return TURRET_ITEMS;
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ModItems() {
    }
}
