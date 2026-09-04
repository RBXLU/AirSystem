package com.rbxlu.airsystem.network.payload;

import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.content.drone.DroneFlight;
import com.rbxlu.airsystem.content.drone.DroneKind;
import com.rbxlu.airsystem.content.drone.DroneState;
import com.rbxlu.airsystem.content.drone.Munition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record DroneSyncPayload(List<Snapshot> drones, List<MunitionSnapshot> munitions)
        implements CustomPacketPayload {
    public record Snapshot(int netId, DroneKind kind, double x, double y, double z,
                           float yaw, float pitch, float roll, DroneState state,
                           boolean engineDead, boolean manual, float throttle) {
        public static final StreamCodec<ByteBuf, Snapshot> STREAM_CODEC = StreamCodec.of(
                (buffer, snapshot) -> {
                    buffer.writeInt(snapshot.netId());
                    buffer.writeByte(snapshot.kind().ordinal());
                    buffer.writeDouble(snapshot.x());
                    buffer.writeDouble(snapshot.y());
                    buffer.writeDouble(snapshot.z());
                    buffer.writeFloat(snapshot.yaw());
                    buffer.writeFloat(snapshot.pitch());
                    buffer.writeFloat(snapshot.roll());
                    buffer.writeByte(snapshot.state().ordinal());
                    int flags = (snapshot.engineDead() ? 1 : 0) | (snapshot.manual() ? 2 : 0);
                    buffer.writeByte(flags);
                    buffer.writeFloat(snapshot.throttle());
                },
                buffer -> {
                    int netId = buffer.readInt();
                    DroneKind kind = DroneKind.byOrdinal(buffer.readByte());
                    double x = buffer.readDouble();
                    double y = buffer.readDouble();
                    double z = buffer.readDouble();
                    float yaw = buffer.readFloat();
                    float pitch = buffer.readFloat();
                    float roll = buffer.readFloat();
                    DroneState state = DroneState.byOrdinal(buffer.readByte());
                    int flags = buffer.readByte();
                    float throttle = buffer.readFloat();
                    return new Snapshot(netId, kind, x, y, z, yaw, pitch, roll, state,
                            (flags & 1) != 0, (flags & 2) != 0, throttle);
                });

        public static Snapshot of(DroneFlight flight) {
            return new Snapshot(flight.netId(), flight.kind(),
                    flight.position().x, flight.position().y, flight.position().z,
                    flight.yaw(), flight.pitch(), flight.roll(), flight.state(),
                    flight.engineDead(), flight.manual(), flight.throttle());
        }
    }

    public record MunitionSnapshot(int netId, double x, double y, double z, float yaw, float pitch) {
        public static final StreamCodec<ByteBuf, MunitionSnapshot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MunitionSnapshot::netId,
                ByteBufCodecs.DOUBLE, MunitionSnapshot::x,
                ByteBufCodecs.DOUBLE, MunitionSnapshot::y,
                ByteBufCodecs.DOUBLE, MunitionSnapshot::z,
                ByteBufCodecs.FLOAT, MunitionSnapshot::yaw,
                ByteBufCodecs.FLOAT, MunitionSnapshot::pitch,
                MunitionSnapshot::new);

        public static MunitionSnapshot of(Munition munition) {
            return new MunitionSnapshot(munition.netId(), munition.position().x,
                    munition.position().y, munition.position().z, munition.yaw(), munition.pitch());
        }
    }

    public static final CustomPacketPayload.Type<DroneSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "drone_sync"));

    public static final StreamCodec<ByteBuf, DroneSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, Snapshot.STREAM_CODEC, 256),
            DroneSyncPayload::drones,
            ByteBufCodecs.collection(ArrayList::new, MunitionSnapshot.STREAM_CODEC, 256),
            DroneSyncPayload::munitions,
            DroneSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
