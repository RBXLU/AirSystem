package com.rbxlu.airsystem.content.radar;

import com.rbxlu.airsystem.content.drone.DroneKind;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RadarContact(int netId, float x, float y, float z, float heading, float speed,
                           int kind, boolean friendly) {
    public static final StreamCodec<ByteBuf, RadarContact> STREAM_CODEC = StreamCodec.of(
            (buffer, contact) -> {
                ByteBufCodecs.VAR_INT.encode(buffer, contact.netId);
                buffer.writeFloat(contact.x);
                buffer.writeFloat(contact.y);
                buffer.writeFloat(contact.z);
                buffer.writeFloat(contact.heading);
                buffer.writeFloat(contact.speed);
                ByteBufCodecs.VAR_INT.encode(buffer, contact.kind);
                buffer.writeBoolean(contact.friendly);
            },
            buffer -> new RadarContact(
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    buffer.readBoolean()));

    public DroneKind droneKind() {
        DroneKind[] values = DroneKind.values();
        return values[Math.floorMod(kind, values.length)];
    }

    /**
     * Radar returns a size, not a type. Only a friendly contact — one launched from
     * inside the station's own perimeter — is identified by name.
     */
    public SizeClass sizeClass() {
        float span = droneKind().getWingspan();
        if (span < 2.0F) {
            return SizeClass.SMALL;
        }
        return span < 3.5F ? SizeClass.MEDIUM : SizeClass.LARGE;
    }

    public enum SizeClass {
        SMALL,
        MEDIUM,
        LARGE;

        public String key() {
            return "screen.airsystem.radar.size." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
