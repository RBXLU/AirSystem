package com.rbxlu.airsystem.network.payload;

import com.rbxlu.airsystem.AirSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DroneFeedPayload(int entityId, boolean open) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DroneFeedPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "drone_feed"));

    public static final StreamCodec<ByteBuf, DroneFeedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DroneFeedPayload::entityId,
            ByteBufCodecs.BOOL, DroneFeedPayload::open,
            DroneFeedPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
