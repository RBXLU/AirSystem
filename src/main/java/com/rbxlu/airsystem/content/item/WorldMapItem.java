package com.rbxlu.airsystem.content.item;

import com.rbxlu.airsystem.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class WorldMapItem extends Item {
    public WorldMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            com.rbxlu.airsystem.client.ClientHooks.openWorldMap(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModDataComponents.TargetPoint mark = stack.get(ModDataComponents.MAP_MARK.get());
        if (mark != null) {
            tooltip.add(Component.translatable("tooltip.airsystem.map.mark", mark.toString())
                    .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("tooltip.airsystem.map.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
