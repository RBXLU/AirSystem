package com.rbxlu.airsystem.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rbxlu.airsystem.AirSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AirSystem.MODID);

    public record TargetPoint(int x, int y, int z, String label) {
        public static final Codec<TargetPoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("x").forGetter(TargetPoint::x),
                Codec.INT.fieldOf("y").forGetter(TargetPoint::y),
                Codec.INT.fieldOf("z").forGetter(TargetPoint::z),
                Codec.STRING.optionalFieldOf("label", "").forGetter(TargetPoint::label)
        ).apply(instance, TargetPoint::new));

        public static final StreamCodec<io.netty.buffer.ByteBuf, TargetPoint> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, TargetPoint::x,
                ByteBufCodecs.VAR_INT, TargetPoint::y,
                ByteBufCodecs.VAR_INT, TargetPoint::z,
                ByteBufCodecs.STRING_UTF8, TargetPoint::label,
                TargetPoint::new);

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }

        @Override
        public String toString() {
            return x + " " + y + " " + z;
        }
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> LINKED_DRONE =
            COMPONENTS.register("linked_drone", () -> DataComponentType.<BlockPos>builder()
                    .persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> LINKED_FLIGHT =
            COMPONENTS.register("linked_flight", () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TargetPoint>> TARGET_POINT =
            COMPONENTS.register("target_point", () -> DataComponentType.<TargetPoint>builder()
                    .persistent(TargetPoint.CODEC)
                    .networkSynchronized(TargetPoint.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TargetPoint>> MAP_MARK =
            COMPONENTS.register("map_mark", () -> DataComponentType.<TargetPoint>builder()
                    .persistent(TargetPoint.CODEC)
                    .networkSynchronized(TargetPoint.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> LINK_SOURCE =
            COMPONENTS.register("link_source", () -> DataComponentType.<BlockPos>builder()
                    .persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> REQUIRE_TARGET =
            COMPONENTS.register("require_target", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORED_AMMO =
            COMPONENTS.register("stored_ammo", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }

    private ModDataComponents() {
    }
}
