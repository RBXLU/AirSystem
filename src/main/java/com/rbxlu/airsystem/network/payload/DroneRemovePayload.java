package com.rbxlu.airsystem.network.payload;

import com.rbxlu.airsystem.AirSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record DroneRemovePayload(List<Integer> netIds) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DroneRemovePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "drone_remove"));

    public static final StreamCodec<ByteBuf, DroneRemovePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT, 256),
            DroneRemovePayload::netIds,
            DroneRemovePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
