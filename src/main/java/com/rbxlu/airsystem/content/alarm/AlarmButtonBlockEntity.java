package com.rbxlu.airsystem.content.alarm;

import com.rbxlu.airsystem.registry.ModBlockEntities;
import com.rbxlu.airsystem.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AlarmButtonBlockEntity extends BlockEntity {
    private static final int AUTO_LINK_RADIUS = 64;

    private final List<BlockPos> linkedSirens = new ArrayList<>();
    private boolean triggered;

    public AlarmButtonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALARM_BUTTON.get(), pos, state);
    }

    public List<BlockPos> getLinkedSirens() {
        return linkedSirens;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public boolean toggleLink(BlockPos siren) {
        boolean added;
        if (linkedSirens.contains(siren)) {
            linkedSirens.remove(siren);
            added = false;
        } else {
            linkedSirens.add(siren);
            added = true;
        }
        setChanged();
        return added;
    }

    public void press(@Nullable Player player) {
        if (level == null || level.isClientSide) {
            return;
        }

        List<AirRaidSirenBlockEntity> sirens = resolveSirens();

        // Read the sirens rather than blind-toggling: an alert can now time out by itself,
        // and a blind toggle would then send all-clear to sirens that are already silent.
        boolean sounding = false;
        for (AirRaidSirenBlockEntity siren : sirens) {
            if (siren.isActive() && !siren.isAllClear()) {
                sounding = true;
                break;
            }
        }
        triggered = !sounding;

        level.playSound(null, worldPosition, ModSounds.ALARM_BUTTON_CLICK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);

        if (sirens.isEmpty()) {
            triggered = false;
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.airsystem.alarm.no_sirens")
                        .withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        for (AirRaidSirenBlockEntity siren : sirens) {
            siren.sound(triggered, null);
        }

        BlockState state = getBlockState();
        if (state.hasProperty(AlarmButtonBlock.TRIGGERED)) {
            level.setBlock(worldPosition, state.setValue(AlarmButtonBlock.TRIGGERED, triggered), 3);
        }

        if (player != null) {
            player.displayClientMessage(Component.translatable(triggered
                            ? "message.airsystem.alarm.raised"
                            : "message.airsystem.alarm.cleared", sirens.size())
                    .withStyle(triggered ? ChatFormatting.RED : ChatFormatting.GREEN), true);
        }
        setChanged();
    }

    private List<AirRaidSirenBlockEntity> resolveSirens() {
        List<AirRaidSirenBlockEntity> result = new ArrayList<>();
        if (level == null) {
            return result;
        }

        if (!linkedSirens.isEmpty()) {
            linkedSirens.removeIf(pos -> !(level.getBlockEntity(pos) instanceof AirRaidSirenBlockEntity));
            for (BlockPos pos : linkedSirens) {
                if (level.getBlockEntity(pos) instanceof AirRaidSirenBlockEntity siren) {
                    result.add(siren);
                }
            }
            return result;
        }

        double radiusSqr = (double) AUTO_LINK_RADIUS * AUTO_LINK_RADIUS;
        for (BlockPos pos : AirRaidSirenBlockEntity.registered(level)) {
            if (pos.distSqr(worldPosition) > radiusSqr) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof AirRaidSirenBlockEntity siren) {
                result.add(siren);
            }
        }
        return result;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        linkedSirens.clear();
        ListTag list = tag.getList("Sirens", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            linkedSirens.add(NbtUtils.readBlockPos(list.getCompound(i), "pos").orElse(BlockPos.ZERO));
        }
        triggered = tag.getBoolean("Triggered");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (BlockPos pos : linkedSirens) {
            CompoundTag entry = new CompoundTag();
            entry.put("pos", NbtUtils.writeBlockPos(pos));
            list.add(entry);
        }
        tag.put("Sirens", list);
        tag.putBoolean("Triggered", triggered);
    }
}
