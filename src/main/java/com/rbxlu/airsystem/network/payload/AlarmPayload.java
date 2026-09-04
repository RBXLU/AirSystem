package com.rbxlu.airsystem.network.payload;

import com.rbxlu.airsystem.AirSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AlarmPayload(int mode, BlockPos source) implements CustomPacketPayload {
    public static final int MODE_ALERT = 0;
    public static final int MODE_ALL_CLEAR = 1;

    public static final CustomPacketPayload.Type<AlarmPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "alarm"));

    public static final StreamCodec<ByteBuf, AlarmPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AlarmPayload::mode,
            BlockPos.STREAM_CODEC, AlarmPayload::source,
            AlarmPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
