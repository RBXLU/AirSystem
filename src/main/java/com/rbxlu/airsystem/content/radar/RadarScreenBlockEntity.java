package com.rbxlu.airsystem.content.radar;

import com.rbxlu.airsystem.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RadarScreenBlockEntity extends BlockEntity {
    private final List<BlockPos> stations = new ArrayList<>();

    public RadarScreenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR_SCREEN.get(), pos, state);
    }

    public List<BlockPos> stations() {
        return stations;
    }

    public boolean toggleStation(BlockPos radar) {
        if (stations.remove(radar)) {
            setChanged();
            return false;
        }
        stations.add(radar);
        setChanged();
        return true;
    }

    /**
     * Contacts from every linked station, deduplicated: two stations covering the
     * same sky must not draw the same aircraft twice.
     */
    public List<RadarContact> contacts() {
        if (level == null) {
            return List.of();
        }
        stations.removeIf(pos -> !(level.getBlockEntity(pos) instanceof RadarBlockEntity));

        Map<Integer, RadarContact> merged = new LinkedHashMap<>();
        for (BlockPos pos : stations) {
            if (level.getBlockEntity(pos) instanceof RadarBlockEntity radar) {
                for (RadarContact contact : radar.contacts()) {
                    merged.putIfAbsent(contact.netId(), contact);
                }
            }
        }
        return List.copyOf(merged.values());
    }

    /** Centre of the scope: the first live station, or the screen itself when none is linked. */
    public BlockPos origin() {
        return stations.isEmpty() ? worldPosition : stations.get(0);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stations.clear();
        ListTag list = tag.getList("Stations", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            stations.add(NbtUtils.readBlockPos(list.getCompound(i), "pos").orElse(BlockPos.ZERO));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (BlockPos pos : stations) {
            CompoundTag entry = new CompoundTag();
            entry.put("pos", NbtUtils.writeBlockPos(pos));
            list.add(entry);
        }
        tag.put("Stations", list);
    }
}
