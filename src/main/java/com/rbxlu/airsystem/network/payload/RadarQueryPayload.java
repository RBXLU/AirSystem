package com.rbxlu.airsystem.network.payload;

import com.rbxlu.airsystem.AirSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RadarQueryPayload(BlockPos screen) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RadarQueryPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "radar_query"));

    public static final StreamCodec<ByteBuf, RadarQueryPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RadarQueryPayload::screen,
            RadarQueryPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
