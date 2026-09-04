package com.rbxlu.airsystem.registry;

import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.content.drone.DroneKind;
import com.rbxlu.airsystem.content.turret.TurretKind;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AirSystem.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.airsystem.main"))
                    .icon(() -> new ItemStack(ModItems.droneItem(DroneKind.SHAHED_136).get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.WORLD_MAP.get());
                        output.accept(ModItems.REMOTE_CONTROL.get());
                        output.accept(ModItems.LINKING_CABLE.get());
                        output.accept(ModItems.AIR_RAID_SIREN.get());
                        output.accept(ModItems.ALARM_BUTTON.get());
                        output.accept(ModItems.RADAR.get());
                        output.accept(ModItems.RADAR_SCREEN.get());

                        for (DroneKind kind : DroneKind.values()) {
                            output.accept(ModItems.droneItem(kind).get());
                        }
                        for (TurretKind kind : TurretKind.values()) {
                            output.accept(ModItems.turretItem(kind).get());
                        }

                        output.accept(ModItems.AMMO_35MM.get());
                        output.accept(ModItems.AMMO_30MM.get());
                        output.accept(ModItems.DRONE_FRAME.get());
                        output.accept(ModItems.ENGINE_MODULE.get());
                        output.accept(ModItems.WARHEAD_MODULE.get());
                        output.accept(ModItems.CAMERA_MODULE.get());
                        output.accept(ModItems.GUIDANCE_MODULE.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }

    private ModCreativeTabs() {
    }
}
