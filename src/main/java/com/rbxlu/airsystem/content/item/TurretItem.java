package com.rbxlu.airsystem.content.item;

import com.rbxlu.airsystem.content.turret.TurretEntity;
import com.rbxlu.airsystem.content.turret.TurretKind;
import com.rbxlu.airsystem.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class TurretItem extends Item {
    private final TurretKind kind;

    public TurretItem(TurretKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public TurretKind getKind() {
        return kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        TurretEntity turret = ModEntities.turretType(kind).get().create(level);
        if (turret == null) {
            return InteractionResult.FAIL;
        }

        float yaw = context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0F;
        turret.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        level.addFreshEntity(turret);
        context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.airsystem.turret.caliber", kind.getCaliber())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.airsystem.turret.range", (int) kind.getRange())
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.airsystem.turret.magazine", kind.getMagazine())
                .withStyle(ChatFormatting.DARK_GRAY));
        if (kind.isAutoOnly()) {
            tooltip.add(Component.translatable("tooltip.airsystem.turret.auto_hint",
                            (int) com.rbxlu.airsystem.util.AirSystemConfig.friendlyRadius())
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.airsystem.turret.hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
