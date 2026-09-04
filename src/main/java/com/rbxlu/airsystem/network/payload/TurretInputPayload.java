package com.rbxlu.airsystem.network.payload;

import com.rbxlu.airsystem.AirSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TurretInputPayload(int entityId, float yaw, float pitch, boolean firing)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TurretInputPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "turret_input"));

    public static final StreamCodec<ByteBuf, TurretInputPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TurretInputPayload::entityId,
            ByteBufCodecs.FLOAT, TurretInputPayload::yaw,
            ByteBufCodecs.FLOAT, TurretInputPayload::pitch,
            ByteBufCodecs.BOOL, TurretInputPayload::firing,
            TurretInputPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
