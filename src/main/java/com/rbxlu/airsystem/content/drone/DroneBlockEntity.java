package com.rbxlu.airsystem.content.drone;

import com.rbxlu.airsystem.registry.ModBlockEntities;
import com.rbxlu.airsystem.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class DroneBlockEntity extends BlockEntity {
    private DroneKind kind = DroneKind.SHAHED_136;
    private BlockPos target = BlockPos.ZERO;

    public DroneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRONE.get(), pos, state);
    }

    public DroneKind getKind() {
        return kind;
    }

    public void setKind(DroneKind kind) {
        this.kind = kind;
        setChanged();
        sync();
    }

    public BlockPos getTarget() {
        return target;
    }

    public void setTarget(BlockPos target) {
        this.target = target;
        setChanged();
    }

    public boolean hasTarget() {
        return !BlockPos.ZERO.equals(target);
    }

    public ItemStack toItem() {
        return new ItemStack(ModItems.droneItem(kind).get());
    }

    public Vec3 launchPosition() {
        return Vec3.atCenterOf(worldPosition).add(0.0D, 0.6D, 0.0D);
    }

    public float launchYaw() {
        return getBlockState().getValue(DroneBlock.FACING).toYRot();
    }

    public float launchYaw(BlockPos target) {
        double dx = target.getX() + 0.5D - (worldPosition.getX() + 0.5D);
        double dz = target.getZ() + 0.5D - (worldPosition.getZ() + 0.5D);
        if (dx == 0.0D && dz == 0.0D) {
            Direction facing = getBlockState().getValue(DroneBlock.FACING);
            return facing.toYRot();
        }
        return (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        kind = DroneKind.byId(tag.getString("Kind"));
        target = new BlockPos(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Kind", kind.getId());
        tag.putInt("TargetX", target.getX());
        tag.putInt("TargetY", target.getY());
        tag.putInt("TargetZ", target.getZ());
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
