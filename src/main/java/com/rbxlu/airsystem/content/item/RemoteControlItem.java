package com.rbxlu.airsystem.content.item;

import com.rbxlu.airsystem.content.drone.DroneBlockEntity;
import com.rbxlu.airsystem.registry.ModDataComponents;
import com.rbxlu.airsystem.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class RemoteControlItem extends Item {
    public RemoteControlItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (!(level.getBlockEntity(pos) instanceof DroneBlockEntity drone)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        stack.set(ModDataComponents.LINKED_DRONE.get(), pos);
        stack.remove(ModDataComponents.LINKED_FLIGHT.get());

        stack.set(ModDataComponents.REQUIRE_TARGET.get(),
                drone.getKind().getRole() != com.rbxlu.airsystem.content.drone.DroneKind.Role.RECON);

        ModDataComponents.TargetPoint target = stack.get(ModDataComponents.TARGET_POINT.get());
        if (target != null) {
            drone.setTarget(target.pos());
        }

        level.playSound(null, pos, ModSounds.REMOTE_BEEP.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.translatable(
                    "message.airsystem.drone.linked",
                    Component.translatable(drone.getKind().getTranslationKey())), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            com.rbxlu.airsystem.client.ClientHooks.openRemoteControl(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos stand = stack.get(ModDataComponents.LINKED_DRONE.get());
        UUID inFlight = stack.get(ModDataComponents.LINKED_FLIGHT.get());
        if (inFlight != null) {
            tooltip.add(Component.translatable("tooltip.airsystem.remote.in_flight")
                    .withStyle(ChatFormatting.AQUA));
        } else if (stand != null) {
            tooltip.add(Component.translatable("tooltip.airsystem.remote.linked_at",
                    stand.getX(), stand.getY(), stand.getZ()).withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.airsystem.remote.not_linked").withStyle(ChatFormatting.RED));
        }

        ModDataComponents.TargetPoint target = stack.get(ModDataComponents.TARGET_POINT.get());
        if (target != null) {
            tooltip.add(Component.translatable("tooltip.airsystem.remote.target", target.toString())
                    .withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.airsystem.remote.no_target").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.airsystem.remote.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
