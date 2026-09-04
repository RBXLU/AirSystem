package com.rbxlu.airsystem.network.payload;

import com.rbxlu.airsystem.AirSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ImpactPayload(double x, double y, double z, float power, boolean incendiary, boolean wreck)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ImpactPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "impact"));

    public static final StreamCodec<ByteBuf, ImpactPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ImpactPayload::x,
            ByteBufCodecs.DOUBLE, ImpactPayload::y,
            ByteBufCodecs.DOUBLE, ImpactPayload::z,
            ByteBufCodecs.FLOAT, ImpactPayload::power,
            ByteBufCodecs.BOOL, ImpactPayload::incendiary,
            ByteBufCodecs.BOOL, ImpactPayload::wreck,
            ImpactPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
