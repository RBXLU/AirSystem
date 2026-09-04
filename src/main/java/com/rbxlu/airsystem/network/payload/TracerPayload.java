package com.rbxlu.airsystem.network.payload;

import com.rbxlu.airsystem.AirSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TracerPayload(double fromX, double fromY, double fromZ,
                            double toX, double toY, double toZ) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TracerPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "tracer"));

    public static final StreamCodec<ByteBuf, TracerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, TracerPayload::fromX,
            ByteBufCodecs.DOUBLE, TracerPayload::fromY,
            ByteBufCodecs.DOUBLE, TracerPayload::fromZ,
            ByteBufCodecs.DOUBLE, TracerPayload::toX,
            ByteBufCodecs.DOUBLE, TracerPayload::toY,
            ByteBufCodecs.DOUBLE, TracerPayload::toZ,
            TracerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
