package com.rbxlu.airsystem.network.payload;

import com.rbxlu.airsystem.AirSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DroneCommandPayload(int entityId, int command) implements CustomPacketPayload {
    public static final int TOGGLE_MANUAL = 0;

    public static final int STRIKE_NOW = 1;

    public static final int CLOSE_FEED = 2;

    public static final int SELF_DESTRUCT = 3;

    public static final int LAND = 4;

    public static final CustomPacketPayload.Type<DroneCommandPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "drone_command"));

    public static final StreamCodec<ByteBuf, DroneCommandPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DroneCommandPayload::entityId,
            ByteBufCodecs.VAR_INT, DroneCommandPayload::command,
            DroneCommandPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
