package com.rbxlu.airsystem.registry;

import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.content.alarm.AirRaidSirenBlock;
import com.rbxlu.airsystem.content.alarm.AlarmButtonBlock;
import com.rbxlu.airsystem.content.drone.DroneBlock;
import com.rbxlu.airsystem.content.radar.RadarBlock;
import com.rbxlu.airsystem.content.radar.RadarScreenBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, AirSystem.MODID);

    public static final DeferredHolder<Block, AirRaidSirenBlock> AIR_RAID_SIREN = BLOCKS.register("air_raid_siren",
            () -> new AirRaidSirenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(AirRaidSirenBlock.ACTIVE) ? 7 : 0)));

    public static final DeferredHolder<Block, AlarmButtonBlock> ALARM_BUTTON = BLOCKS.register("alarm_button",
            () -> new AlarmButtonBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(AlarmButtonBlock.TRIGGERED) ? 10 : 3)));

    public static final DeferredHolder<Block, DroneBlock> DRONE = BLOCKS.register("drone",
            () -> new DroneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(1.5F, 3.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    public static final DeferredHolder<Block, RadarBlock> RADAR = BLOCKS.register("radar",
            () -> new RadarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.5F, 8.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)));

    public static final DeferredHolder<Block, RadarScreenBlock> RADAR_SCREEN = BLOCKS.register("radar_screen",
            () -> new RadarScreenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 6)));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private ModBlocks() {
    }
}
