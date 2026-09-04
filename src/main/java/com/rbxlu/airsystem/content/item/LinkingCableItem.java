package com.rbxlu.airsystem.content.item;

import com.rbxlu.airsystem.content.alarm.AlarmButtonBlockEntity;
import com.rbxlu.airsystem.content.alarm.AirRaidSirenBlockEntity;
import com.rbxlu.airsystem.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class LinkingCableItem extends Item {
    public LinkingCableItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof AirRaidSirenBlockEntity) {
            stack.set(ModDataComponents.LINK_SOURCE.get(), pos);
            player.displayClientMessage(Component.translatable("message.airsystem.cable.siren_selected",
                    pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }

        if (blockEntity instanceof AlarmButtonBlockEntity button) {
            BlockPos siren = stack.get(ModDataComponents.LINK_SOURCE.get());
            if (siren == null) {
                player.displayClientMessage(Component.translatable("message.airsystem.cable.no_siren")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            if (!(level.getBlockEntity(siren) instanceof AirRaidSirenBlockEntity)) {
                player.displayClientMessage(Component.translatable("message.airsystem.cable.siren_gone")
                        .withStyle(ChatFormatting.RED), true);
                stack.remove(ModDataComponents.LINK_SOURCE.get());
                return InteractionResult.CONSUME;
            }

            boolean added = button.toggleLink(siren);
            stack.remove(ModDataComponents.LINK_SOURCE.get());
            player.displayClientMessage(Component.translatable(added
                            ? "message.airsystem.cable.linked"
                            : "message.airsystem.cable.unlinked",
                    button.getLinkedSirens().size()).withStyle(ChatFormatting.GREEN), true);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos source = stack.get(ModDataComponents.LINK_SOURCE.get());
        if (source != null) {
            tooltip.add(Component.translatable("tooltip.airsystem.cable.selected",
                    source.getX(), source.getY(), source.getZ()).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.airsystem.cable.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
