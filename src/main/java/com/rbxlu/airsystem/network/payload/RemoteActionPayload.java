package com.rbxlu.airsystem.network.payload;

import com.rbxlu.airsystem.AirSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RemoteActionPayload(int action, BlockPos target, String label) implements CustomPacketPayload {
    public static final int SET_TARGET = 0;

    public static final int LAUNCH = 1;

    public static final int MARK_MAP = 2;

    public static final int SET_REQUIRE_TARGET = 3;

    public static final CustomPacketPayload.Type<RemoteActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "remote_action"));

    public static final StreamCodec<ByteBuf, RemoteActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RemoteActionPayload::action,
            BlockPos.STREAM_CODEC, RemoteActionPayload::target,
            ByteBufCodecs.stringUtf8(64), RemoteActionPayload::label,
            RemoteActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
