package com.rbxlu.airsystem.content.item;

import com.rbxlu.airsystem.content.drone.DroneBlock;
import com.rbxlu.airsystem.content.drone.DroneBlockEntity;
import com.rbxlu.airsystem.content.drone.DroneKind;
import com.rbxlu.airsystem.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class DroneItem extends Item {
    private final DroneKind kind;

    public DroneItem(DroneKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public DroneKind getKind() {
        return kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

        if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        if (!level.getBlockState(pos.below()).isSolid()) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Direction facing = context.getPlayer() != null
                ? context.getPlayer().getDirection().getOpposite()
                : Direction.NORTH;
        level.setBlockAndUpdate(pos, ModBlocks.DRONE.get().defaultBlockState()
                .setValue(DroneBlock.FACING, facing));

        if (level.getBlockEntity(pos) instanceof DroneBlockEntity drone) {
            drone.setKind(kind);
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NETHERITE_BLOCK_PLACE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.2F);
        context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.airsystem.drone.role_" + kind.getRole().name().toLowerCase())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.airsystem.drone.speed",
                String.format("%.0f", kind.getCruiseSpeed() * 20.0F)).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.airsystem.drone.ceiling", kind.getCeiling())
                .withStyle(ChatFormatting.DARK_GRAY));
        if (kind.hasWarhead()) {
            tooltip.add(Component.translatable("tooltip.airsystem.drone.warhead",
                    String.format("%.1f", kind.getWarheadPower())).withStyle(ChatFormatting.RED));
            if (kind.isIncendiary()) {
                tooltip.add(Component.translatable("tooltip.airsystem.drone.incendiary")
                        .withStyle(ChatFormatting.GOLD));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.airsystem.drone.recon").withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.airsystem.drone.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
