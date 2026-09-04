package com.rbxlu.airsystem.client.screen;

import com.rbxlu.airsystem.network.payload.RemoteActionPayload;
import com.rbxlu.airsystem.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.UUID;

public class RemoteControlScreen extends Screen {
    private static final int PANEL_WIDTH = 230;
    private static final int PANEL_HEIGHT = 184;

    private final InteractionHand hand;

    private EditBox coordinatesBox;
    private Button startButton;
    private Button modeButton;
    private Button pasteButton;
    private Button saveButton;

    private boolean requireTarget = true;
    @Nullable
    private Component status;
    private int statusColor = 0xFFFFFFFF;

    public RemoteControlScreen(InteractionHand hand) {
        super(Component.translatable("screen.airsystem.remote"));
        this.hand = hand;
    }

    private int left() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return (height - PANEL_HEIGHT) / 2;
    }

    @Override
    protected void init() {
        int x = left();
        int y = top();

        Boolean stored = remoteStack().get(ModDataComponents.REQUIRE_TARGET.get());
        requireTarget = stored == null || stored;

        modeButton = Button.builder(modeLabel(), button -> toggleMode())
                .bounds(x + 12, y + 30, PANEL_WIDTH - 24, 20).build();
        addRenderableWidget(modeButton);

        coordinatesBox = new EditBox(font, x + 12, y + 80, PANEL_WIDTH - 24, 20,
                Component.translatable("screen.airsystem.remote.coordinates"));
        coordinatesBox.setMaxLength(48);
        coordinatesBox.setHint(Component.literal("x y z"));

        ModDataComponents.TargetPoint saved = savedTarget();
        if (saved != null) {
            coordinatesBox.setValue(saved.toString());
        }
        addRenderableWidget(coordinatesBox);

        pasteButton = Button.builder(Component.translatable("screen.airsystem.remote.paste"),
                        button -> pasteFromClipboard())
                .bounds(x + 12, y + 106, 104, 20).build();
        addRenderableWidget(pasteButton);

        saveButton = Button.builder(Component.translatable("screen.airsystem.remote.save"),
                        button -> saveTarget())
                .bounds(x + 122, y + 106, 96, 20).build();
        addRenderableWidget(saveButton);

        startButton = Button.builder(Component.translatable("screen.airsystem.remote.start"),
                        button -> start())
                .bounds(x + 12, y + 132, PANEL_WIDTH - 24, 24).build();
        addRenderableWidget(startButton);

        applyMode();
        setInitialFocus(coordinatesBox);
    }

    private Component modeLabel() {
        return Component.translatable(requireTarget
                ? "screen.airsystem.remote.mode_target"
                : "screen.airsystem.remote.mode_free");
    }

    private void toggleMode() {
        requireTarget = !requireTarget;
        PacketDistributor.sendToServer(new RemoteActionPayload(
                RemoteActionPayload.SET_REQUIRE_TARGET, BlockPos.ZERO, requireTarget ? "1" : "0"));
        applyMode();
    }

    private void applyMode() {
        modeButton.setMessage(modeLabel());
        coordinatesBox.setEditable(requireTarget);
        coordinatesBox.visible = requireTarget;
        pasteButton.visible = requireTarget;
        saveButton.visible = requireTarget;
        status = null;
    }

    private ItemStack remoteStack() {
        return minecraft != null && minecraft.player != null
                ? minecraft.player.getItemInHand(hand)
                : ItemStack.EMPTY;
    }

    @Nullable
    private ModDataComponents.TargetPoint savedTarget() {
        return remoteStack().get(ModDataComponents.TARGET_POINT.get());
    }

    private void pasteFromClipboard() {
        if (minecraft == null) {
            return;
        }
        String clipboard = minecraft.keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isBlank()) {
            setStatus("screen.airsystem.remote.clipboard_empty", 0xFFFF5555);
            return;
        }
        coordinatesBox.setValue(clipboard.trim());
        setStatus("screen.airsystem.remote.pasted", 0xFF55FF55);
    }

    @Nullable
    private BlockPos parseCoordinates(String raw) {
        String cleaned = raw.replace("/tp", " ")
                .replace(",", " ")
                .replace("[", " ")
                .replace("]", " ")
                .replace("~", " ")
                .trim();
        String[] parts = cleaned.split("\\s+");
        if (parts.length < 3) {
            return null;
        }
        try {
            int x = (int) Math.floor(Double.parseDouble(parts[parts.length - 3]));
            int y = (int) Math.floor(Double.parseDouble(parts[parts.length - 2]));
            int z = (int) Math.floor(Double.parseDouble(parts[parts.length - 1]));
            return new BlockPos(x, y, z);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean saveTarget() {
        BlockPos target = parseCoordinates(coordinatesBox.getValue());
        if (target == null) {
            setStatus("screen.airsystem.remote.bad_coordinates", 0xFFFF5555);
            return false;
        }
        PacketDistributor.sendToServer(new RemoteActionPayload(
                RemoteActionPayload.SET_TARGET, target, hand.name()));
        setStatus("screen.airsystem.remote.saved", 0xFF55FF55);
        return true;
    }

    private void start() {
        if (requireTarget && !saveTarget()) {
            return;
        }
        PacketDistributor.sendToServer(new RemoteActionPayload(
                RemoteActionPayload.LAUNCH, BlockPos.ZERO, hand.name()));
        onClose();
    }

    private void setStatus(String key, int color) {
        status = Component.translatable(key);
        statusColor = color;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Panel before the widgets: the other way round its fill covers button frames.
        renderPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPanel(GuiGraphics graphics) {
        int x = left();
        int y = top();

        graphics.fill(x - 2, y - 2, x + PANEL_WIDTH + 2, y + PANEL_HEIGHT + 2, 0xFF15181A);
        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF24282B);
        graphics.fill(x, y, x + PANEL_WIDTH, y + 20, 0xFF1B4D2E);

        graphics.drawCenteredString(font, title, x + PANEL_WIDTH / 2, y + 6, 0xFFE0E0E0);

        BlockPos stand = remoteStack().get(ModDataComponents.LINKED_DRONE.get());
        UUID inFlight = remoteStack().get(ModDataComponents.LINKED_FLIGHT.get());
        Component droneStatus;
        if (inFlight != null) {
            droneStatus = Component.translatable("screen.airsystem.remote.drone_in_flight")
                    .withStyle(ChatFormatting.AQUA);
        } else if (stand != null) {
            droneStatus = Component.translatable("screen.airsystem.remote.drone_connected")
                    .withStyle(ChatFormatting.GREEN);
        } else {
            droneStatus = Component.translatable("screen.airsystem.remote.drone_missing")
                    .withStyle(ChatFormatting.RED);
        }
        graphics.drawString(font, droneStatus, x + 12, y + 22, 0xFFFFFFFF, false);

        ModDataComponents.TargetPoint saved = savedTarget();
        Component targetStatus = !requireTarget
                ? Component.translatable("screen.airsystem.remote.target_not_needed")
                        .withStyle(ChatFormatting.AQUA)
                : saved != null
                ? Component.translatable("screen.airsystem.remote.current_target", saved.toString())
                .withStyle(ChatFormatting.GOLD)
                : Component.translatable("screen.airsystem.remote.no_target").withStyle(ChatFormatting.GRAY);
        graphics.drawString(font, targetStatus, x + 12, y + 56, 0xFFFFFFFF, false);

        graphics.drawString(font, Component.translatable(requireTarget
                        ? "screen.airsystem.remote.coordinates"
                        : "screen.airsystem.remote.manual_hint"),
                x + 12, y + 68, 0xFFAAAAAA, false);

        if (status != null) {
            graphics.drawCenteredString(font, status, x + PANEL_WIDTH / 2, y + 162, statusColor);
        } else {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.airsystem.remote.hint").withStyle(ChatFormatting.DARK_GRAY),
                    x + PANEL_WIDTH / 2, y + 162, 0xFF777777);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
