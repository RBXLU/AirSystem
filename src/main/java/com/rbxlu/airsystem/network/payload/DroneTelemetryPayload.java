package com.rbxlu.airsystem.network.payload;

import com.rbxlu.airsystem.AirSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DroneTelemetryPayload(int netId, BlockPos target, int fuel, int munitions, int hitsLeft)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DroneTelemetryPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "drone_telemetry"));

    public static final StreamCodec<ByteBuf, DroneTelemetryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DroneTelemetryPayload::netId,
            BlockPos.STREAM_CODEC, DroneTelemetryPayload::target,
            ByteBufCodecs.VAR_INT, DroneTelemetryPayload::fuel,
            ByteBufCodecs.VAR_INT, DroneTelemetryPayload::munitions,
            ByteBufCodecs.VAR_INT, DroneTelemetryPayload::hitsLeft,
            DroneTelemetryPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
