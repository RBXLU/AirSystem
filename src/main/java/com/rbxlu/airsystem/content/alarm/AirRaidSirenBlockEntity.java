package com.rbxlu.airsystem.content.alarm;

import com.rbxlu.airsystem.network.payload.AlarmPayload;
import com.rbxlu.airsystem.registry.ModBlockEntities;
import com.rbxlu.airsystem.registry.ModSounds;
import com.rbxlu.airsystem.util.AirSystemConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AirRaidSirenBlockEntity extends BlockEntity {
    private static final int ALERT_PERIOD = 180;
    private static final int ALL_CLEAR_PERIOD = 140;

    private static final int ALL_CLEAR_DURATION = 400;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> REGISTRY = new HashMap<>();

    private boolean active;
    private boolean allClear;
    private int timer;

    private int remaining;

    public AirRaidSirenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIR_RAID_SIREN.get(), pos, state);
    }

    public static Set<BlockPos> registered(Level level) {
        return REGISTRY.getOrDefault(level.dimension(), Set.of());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            REGISTRY.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(worldPosition.immutable());
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            Set<BlockPos> positions = REGISTRY.get(level.dimension());
            if (positions != null) {
                positions.remove(worldPosition);
            }
        }
        super.setRemoved();
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAllClear() {
        return allClear;
    }

    public void toggle(@Nullable Player player) {
        if (active) {
            sound(false, player);
        } else {
            sound(true, player);
        }
    }

    public void sound(boolean alert, @Nullable Player initiator) {
        if (level == null || level.isClientSide) {
            return;
        }

        this.active = true;
        this.allClear = !alert;
        this.timer = 0;

        // All-clear is always finite; alert length comes from config, 0 meaning unlimited.
        this.remaining = alert ? AirSystemConfig.alertDurationTicks() : ALL_CLEAR_DURATION;

        updateBlockState(true);
        broadcast(alert);
        playSignal();

        if (initiator != null) {
            initiator.displayClientMessage(Component.translatable(alert
                    ? "message.airsystem.alarm.on"
                    : "message.airsystem.alarm.off"), true);
        }
        setChanged();
    }

    public void silence() {
        if (level == null || level.isClientSide || !active) {
            return;
        }
        active = false;
        allClear = false;
        timer = 0;
        remaining = 0;
        updateBlockState(false);
        setChanged();
    }

    public void serverTick() {
        if (!active || level == null) {
            return;
        }

        timer++;
        int period = allClear ? ALL_CLEAR_PERIOD : ALERT_PERIOD;
        if (timer >= period) {
            timer = 0;
            playSignal();
            broadcast(!allClear);
        }

        if (remaining > 0) {
            remaining--;
            if (remaining == 0) {
                silence();
            }
        }
    }

    private void playSignal() {
        if (level == null) {
            return;
        }
        var sound = allClear ? ModSounds.SIREN_ALL_CLEAR.get() : ModSounds.SIREN_ALERT.get();

        float volume = AirSystemConfig.sirenRadius() / 16.0F;
        level.playSound(null, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D, sound, SoundSource.BLOCKS, volume, 1.0F);
    }

    private void broadcast(boolean alert) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AlarmPayload payload = new AlarmPayload(
                alert ? AlarmPayload.MODE_ALERT : AlarmPayload.MODE_ALL_CLEAR, worldPosition);
        double radiusSqr = (double) AirSystemConfig.sirenRadius() * AirSystemConfig.sirenRadius();
        for (ServerPlayer player : server.players()) {
            if (player.blockPosition().distSqr(worldPosition) <= radiusSqr) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    private void updateBlockState(boolean value) {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(AirRaidSirenBlock.ACTIVE) && state.getValue(AirRaidSirenBlock.ACTIVE) != value) {
            level.setBlock(worldPosition, state.setValue(AirRaidSirenBlock.ACTIVE, value), 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        active = tag.getBoolean("Active");
        allClear = tag.getBoolean("AllClear");
        timer = tag.getInt("Timer");
        remaining = tag.getInt("Remaining");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Active", active);
        tag.putBoolean("AllClear", allClear);
        tag.putInt("Timer", timer);
        tag.putInt("Remaining", remaining);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
