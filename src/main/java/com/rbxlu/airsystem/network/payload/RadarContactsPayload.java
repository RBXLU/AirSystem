package com.rbxlu.airsystem.network.payload;

import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.content.radar.RadarContact;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record RadarContactsPayload(BlockPos origin, int range, int stations, List<RadarContact> contacts)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RadarContactsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "radar_contacts"));

    public static final StreamCodec<ByteBuf, RadarContactsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, RadarContactsPayload::origin,
            ByteBufCodecs.VAR_INT, RadarContactsPayload::range,
            ByteBufCodecs.VAR_INT, RadarContactsPayload::stations,
            RadarContact.STREAM_CODEC.apply(ByteBufCodecs.list()), RadarContactsPayload::contacts,
            RadarContactsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
