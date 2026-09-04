package com.rbxlu.airsystem.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.rbxlu.airsystem.content.drone.DroneFlight;
import com.rbxlu.airsystem.content.drone.DroneFlightManager;
import com.rbxlu.airsystem.content.drone.DroneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class AirSystemCommands {
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("airsystem")
                .requires(source -> source.hasPermission(2));

        root.then(Commands.literal("flights").executes(context -> listFlights(context.getSource())));
        root.then(Commands.literal("abort").executes(context -> abort(context.getSource())));

        LiteralArgumentBuilder<CommandSourceStack> launch = Commands.literal("launch");
        for (DroneKind kind : DroneKind.values()) {
            launch.then(Commands.literal(kind.getId())
                    .then(Commands.argument("target", BlockPosArgument.blockPos())
                            .executes(context -> launch(context.getSource(), kind,
                                    BlockPosArgument.getLoadedBlockPos(context, "target")))));
        }
        root.then(launch);

        event.getDispatcher().register(root);
    }

    private static int listFlights(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        DroneFlightManager manager = DroneFlightManager.get(level);

        if (manager.flights().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.airsystem.flights.empty"), false);
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.airsystem.flights.header",
                manager.flights().size()).withStyle(ChatFormatting.GOLD), false);
        for (DroneFlight flight : manager.flights()) {
            BlockPos position = BlockPos.containing(flight.position());
            source.sendSuccess(() -> Component.literal(" • ")
                    .append(Component.translatable(flight.kind().getTranslationKey()))
                    .append(Component.literal(" [%d %d %d] %s".formatted(
                            position.getX(), position.getY(), position.getZ(),
                            Component.translatable("state.airsystem."
                                    + flight.state().name().toLowerCase()).getString())))
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return manager.flights().size();
    }

    private static int abort(CommandSourceStack source) {
        int count = DroneFlightManager.get(source.getLevel()).abortAll(source.getLevel());
        source.sendSuccess(() -> Component.translatable("command.airsystem.abort", count), true);
        return count;
    }

    private static int launch(CommandSourceStack source, DroneKind kind, BlockPos target) {
        ServerLevel level = source.getLevel();
        var position = source.getPosition().add(0.0D, 12.0D, 0.0D);

        double dx = target.getX() + 0.5D - position.x;
        double dz = target.getZ() + 0.5D - position.z;
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;

        DroneFlightManager.get(level).launch(level, kind, position, yaw, target,
                source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player ? player : null);
        source.sendSuccess(() -> Component.translatable("command.airsystem.launched",
                Component.translatable(kind.getTranslationKey()),
                target.getX(), target.getY(), target.getZ()), true);
        return 1;
    }

    private AirSystemCommands() {
    }
}
