package com.rbxlu.airsystem.network;

import com.rbxlu.airsystem.content.drone.DroneBlockEntity;
import com.rbxlu.airsystem.content.drone.DroneFlight;
import com.rbxlu.airsystem.content.drone.DroneFlightManager;
import com.rbxlu.airsystem.content.drone.DroneKind;
import com.rbxlu.airsystem.content.drone.DroneState;
import com.rbxlu.airsystem.content.drone.Munition;
import com.rbxlu.airsystem.content.item.RemoteControlItem;
import com.rbxlu.airsystem.content.item.WorldMapItem;
import com.rbxlu.airsystem.content.turret.TurretEntity;
import com.rbxlu.airsystem.network.payload.DroneCommandPayload;
import com.rbxlu.airsystem.network.payload.DroneInputPayload;
import com.rbxlu.airsystem.network.payload.RemoteActionPayload;
import com.rbxlu.airsystem.network.payload.TurretInputPayload;
import com.rbxlu.airsystem.registry.ModDataComponents;
import com.rbxlu.airsystem.registry.ModSounds;
import com.rbxlu.airsystem.util.AirSystemConfig;
import com.rbxlu.airsystem.util.DroneFeedSessions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.UUID;

public final class ServerPayloadHandler {
    public static void handleDroneInput(DroneInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            DroneFlight flight = findFlight(player, payload.entityId());
            if (flight != null && flight.manual() && flight.isOperator(player)) {
                flight.setInput(payload.inputMask());
            }
        });
    }

    public static void handleDroneCommand(DroneCommandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            DroneFlight flight = findFlight(player, payload.entityId());
            if (flight == null || !flight.isOperator(player)) {
                return;
            }
            ServerLevel level = player.serverLevel();

            switch (payload.command()) {
                case DroneCommandPayload.TOGGLE_MANUAL -> {
                    boolean manual = !flight.manual();
                    flight.setManual(manual);
                    player.displayClientMessage(Component.translatable(manual
                            ? "message.airsystem.drone.manual_on"
                            : "message.airsystem.drone.manual_off"), true);
                }
                case DroneCommandPayload.STRIKE_NOW -> {
                    if (flight.kind().getRole() == DroneKind.Role.STRIKE) {
                        DroneFlightManager manager = DroneFlightManager.get(level);
                        Munition munition = flight.releaseMunition(manager.nextNetId());
                        if (munition == null) {
                            fail(player, "message.airsystem.drone.no_munitions");
                        } else {
                            manager.addMunition(munition);
                            level.playSound(null, flight.position().x, flight.position().y,
                                    flight.position().z, ModSounds.DRONE_LAUNCH.get(),
                                    SoundSource.NEUTRAL, 3.0F, 1.4F);
                        }
                    } else if (flight.kind().hasWarhead() && flight.state().isControllable()
                            && flight.hasTarget()) {
                        flight.setState(DroneState.DIVE);
                    }
                }
                case DroneCommandPayload.LAND -> {
                    if (flight.beginReturn(level)) {
                        player.displayClientMessage(
                                Component.translatable("message.airsystem.drone.landing")
                                        .withStyle(ChatFormatting.GREEN), true);
                    } else {
                        fail(player, flight.kind().canRecover()
                                ? "message.airsystem.drone.landing_impossible"
                                : "message.airsystem.drone.landing_one_way");
                    }
                }
                case DroneCommandPayload.CLOSE_FEED -> DroneFeedSessions.close(player);
                case DroneCommandPayload.SELF_DESTRUCT -> flight.detonate(level, flight.position());
                default -> {
                }
            }
        });
    }

    public static void handleRemoteAction(RemoteActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            switch (payload.action()) {
                case RemoteActionPayload.SET_TARGET -> setTarget(player, payload.target(), payload.label());
                case RemoteActionPayload.LAUNCH -> launch(player);
                case RemoteActionPayload.MARK_MAP -> markMap(player, payload.target(), payload.label());
                case RemoteActionPayload.SET_REQUIRE_TARGET ->
                        setRequireTarget(player, "1".equals(payload.label()));
                default -> {
                }
            }
        });
    }

    public static void handleTurretInput(TurretInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(payload.entityId());
            if (entity instanceof TurretEntity turret && turret.getControllingPassenger() == player) {
                turret.setFiring(payload.firing());
            }
        });
    }

    private static void setTarget(ServerPlayer player, BlockPos target, String label) {
        ItemStack remote = findHeld(player, RemoteControlItem.class);
        if (remote.isEmpty()) {
            return;
        }
        remote.set(ModDataComponents.TARGET_POINT.get(),
                new ModDataComponents.TargetPoint(target.getX(), target.getY(), target.getZ(), label));

        BlockPos stand = remote.get(ModDataComponents.LINKED_DRONE.get());
        if (stand != null && player.level().getBlockEntity(stand) instanceof DroneBlockEntity drone) {
            drone.setTarget(target);
        }

        player.playNotifySound(ModSounds.REMOTE_BEEP.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        player.displayClientMessage(Component.translatable("message.airsystem.remote.target_set",
                target.getX(), target.getY(), target.getZ()).withStyle(ChatFormatting.GOLD), true);
    }

    private static void markMap(ServerPlayer player, BlockPos target, String label) {
        ItemStack map = findHeld(player, WorldMapItem.class);
        if (map.isEmpty()) {
            return;
        }
        map.set(ModDataComponents.MAP_MARK.get(),
                new ModDataComponents.TargetPoint(target.getX(), target.getY(), target.getZ(), label));
        player.playNotifySound(ModSounds.MAP_MARK.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
    }

    private static void setRequireTarget(ServerPlayer player, boolean require) {
        ItemStack remote = findHeld(player, RemoteControlItem.class);
        if (remote.isEmpty()) {
            return;
        }
        remote.set(ModDataComponents.REQUIRE_TARGET.get(), require);
    }

    // An explicit choice on the remote wins; without one, only scouts fly untasked.
    private static boolean requireTarget(ItemStack remote, DroneKind kind) {
        Boolean explicit = remote.get(ModDataComponents.REQUIRE_TARGET.get());
        if (explicit != null) {
            return explicit;
        }
        return kind.getRole() != DroneKind.Role.RECON;
    }

    private static void launch(ServerPlayer player) {
        ItemStack remote = findHeld(player, RemoteControlItem.class);
        if (remote.isEmpty()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        DroneFlightManager manager = DroneFlightManager.get(level);

        UUID inFlight = remote.get(ModDataComponents.LINKED_FLIGHT.get());
        if (inFlight != null) {
            DroneFlight existing = manager.byId(inFlight);
            if (existing != null && existing.isAlive()) {
                DroneFeedSessions.open(player, existing);
                return;
            }
            remote.remove(ModDataComponents.LINKED_FLIGHT.get());
        }

        BlockPos stand = remote.get(ModDataComponents.LINKED_DRONE.get());
        ModDataComponents.TargetPoint target = remote.get(ModDataComponents.TARGET_POINT.get());

        if (stand == null) {
            fail(player, "message.airsystem.remote.error_no_drone");
            return;
        }
        if (!(level.getBlockEntity(stand) instanceof DroneBlockEntity drone)) {
            fail(player, "message.airsystem.remote.error_lost");
            remote.remove(ModDataComponents.LINKED_DRONE.get());
            return;
        }

        boolean needsTarget = requireTarget(remote, drone.getKind());
        if (needsTarget && target == null) {
            fail(player, "message.airsystem.remote.error_no_target");
            return;
        }
        if (Math.sqrt(stand.distToCenterSqr(player.position())) > AirSystemConfig.controlRange()) {
            fail(player, "message.airsystem.remote.error_range");
            return;
        }
        if (manager.flights().size() >= AirSystemConfig.maxFlights()) {
            fail(player, "message.airsystem.remote.error_too_many");
            return;
        }

        boolean freeFlight = !needsTarget || target == null;
        BlockPos aim = freeFlight ? BlockPos.ZERO : target.pos();
        float yaw = freeFlight ? drone.launchYaw() : drone.launchYaw(aim);

        DroneFlight flight = manager.launch(level, drone.getKind(), drone.launchPosition(),
                yaw, aim, stand, player);
        if (freeFlight) {
            flight.setManual(true);
        }
        level.removeBlock(stand, false);
        remote.set(ModDataComponents.LINKED_FLIGHT.get(), flight.id());
        remote.remove(ModDataComponents.LINKED_DRONE.get());

        level.playSound(null, stand, ModSounds.DRONE_LAUNCH.get(), SoundSource.NEUTRAL, 6.0F, 1.0F);
        player.displayClientMessage(freeFlight
                ? Component.translatable("message.airsystem.remote.launched_free")
                        .withStyle(ChatFormatting.AQUA)
                : Component.translatable("message.airsystem.remote.launched",
                        target.x(), target.y(), target.z()).withStyle(ChatFormatting.GREEN), true);
        DroneFeedSessions.open(player, flight);
    }

    private static void fail(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
        player.playNotifySound(ModSounds.REMOTE_ERROR.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static ItemStack findHeld(ServerPlayer player, Class<?> type) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (type.isInstance(stack.getItem())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    private static DroneFlight findFlight(ServerPlayer player, int netId) {
        return DroneFlightManager.get(player.serverLevel()).byNetId(netId);
    }

    private ServerPayloadHandler() {
    }
}
