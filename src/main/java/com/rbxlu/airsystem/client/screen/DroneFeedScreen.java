package com.rbxlu.airsystem.client.screen;

import com.rbxlu.airsystem.client.ClientDroneStore;
import com.rbxlu.airsystem.client.ClientTelemetry;
import com.rbxlu.airsystem.content.drone.DroneFlight;
import com.rbxlu.airsystem.content.drone.DroneKind;
import com.rbxlu.airsystem.network.payload.DroneCommandPayload;
import com.rbxlu.airsystem.network.payload.DroneInputPayload;
import com.rbxlu.airsystem.util.AirSystemConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

public class DroneFeedScreen extends Screen {
    private static final int HUD_COLOR = 0xFF6FE26F;
    private static final int HUD_DIM = 0xFF3C8C3C;
    private static final int WARN_COLOR = 0xFFFF5555;

    private final int netId;
    private Button manualButton;
    private Button lookButton;
    private Button landButton;
    private int lastInputMask = -1;
    private boolean freeLook;
    private int missingTicks;

    public DroneFeedScreen(int netId) {
        super(Component.translatable("screen.airsystem.drone_feed"));
        this.netId = netId;
    }

    @Nullable
    public ClientDroneStore.ClientDrone drone() {
        return ClientDroneStore.byNetId(netId);
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        manualButton = Button.builder(manualLabel(), button -> command(DroneCommandPayload.TOGGLE_MANUAL))
                .bounds(centerX - 80, 8, 160, 20).build();
        addRenderableWidget(manualButton);

        ClientDroneStore.ClientDrone drone = drone();
        boolean canStrike = drone != null && (drone.kind().hasWarhead()
                || drone.kind().getRole() == DroneKind.Role.STRIKE);
        if (canStrike) {
            addRenderableWidget(Button.builder(Component.translatable("screen.airsystem.feed.strike"),
                            button -> command(DroneCommandPayload.STRIKE_NOW))
                    .bounds(centerX + 84, 8, 70, 20).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.airsystem.feed.disconnect"),
                        button -> onClose())
                .bounds(centerX - 154, 8, 70, 20).build());

        if (drone != null && drone.kind().canRecover()) {
            landButton = Button.builder(Component.translatable("screen.airsystem.feed.land"),
                            button -> command(DroneCommandPayload.LAND))
                    .bounds(centerX - 154, 32, 70, 20).build();
            addRenderableWidget(landButton);
        }

        lookButton = Button.builder(lookLabel(), button -> {
            freeLook = !freeLook;
            button.setMessage(lookLabel());
        }).bounds(centerX + 84, 32, 70, 20).build();
        addRenderableWidget(lookButton);
    }

    private Component manualLabel() {
        ClientDroneStore.ClientDrone drone = drone();
        return Component.translatable(drone != null && drone.manual()
                ? "screen.airsystem.feed.manual_on"
                : "screen.airsystem.feed.manual_off");
    }

    private Component lookLabel() {
        return Component.translatable(freeLook
                ? "screen.airsystem.feed.look_free"
                : "screen.airsystem.feed.look_locked");
    }

    private void command(int command) {
        PacketDistributor.sendToServer(new DroneCommandPayload(netId, command));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void tick() {
        ClientDroneStore.ClientDrone drone = drone();
        if (drone == null) {
            if (++missingTicks > 40) {
                onClose();
            }
            return;
        }
        missingTicks = 0;

        if (manualButton != null) {
            manualButton.setMessage(manualLabel());

            manualButton.active = !drone.state().isRecovering();
        }
        if (landButton != null) {
            landButton.active = !drone.state().isRecovering() && !drone.engineDead();
        }

        if (!freeLook && minecraft != null && minecraft.player != null) {
            minecraft.player.setYRot(drone.lerpYaw(1.0F));
            minecraft.player.setXRot(drone.lerpPitch(1.0F));
        }
        sendInput(drone);
    }

    private void sendInput(ClientDroneStore.ClientDrone drone) {
        if (!drone.manual() || minecraft == null) {
            if (lastInputMask != 0) {
                lastInputMask = 0;
                PacketDistributor.sendToServer(new DroneInputPayload(netId, 0));
            }
            return;
        }

        long window = minecraft.getWindow().getWindow();
        int mask = 0;
        if (isKeyDown(window, GLFW.GLFW_KEY_W)) {
            mask |= DroneFlight.INPUT_FORWARD;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_S)) {
            mask |= DroneFlight.INPUT_BACK;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_A)) {
            mask |= DroneFlight.INPUT_LEFT;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_D)) {
            mask |= DroneFlight.INPUT_RIGHT;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_SPACE)) {
            mask |= DroneFlight.INPUT_BOOST;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)) {
            mask |= DroneFlight.INPUT_BRAKE;
        }

        if (mask != lastInputMask) {
            lastInputMask = mask;
            PacketDistributor.sendToServer(new DroneInputPayload(netId, mask));
        }
    }

    private static boolean isKeyDown(long window, int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        ClientDroneStore.ClientDrone drone = drone();
        if (drone == null) {
            graphics.drawCenteredString(font, Component.translatable("screen.airsystem.feed.no_signal")
                    .withStyle(ChatFormatting.BOLD), width / 2, height / 2, WARN_COLOR);
            return;
        }

        renderFrame(graphics, drone);
        renderTelemetry(graphics, drone);
        renderHeading(graphics, drone);
        renderReticle(graphics);
        if (drone.manual()) {
            graphics.drawCenteredString(font, Component.translatable("screen.airsystem.feed.controls"),
                    width / 2, height - 40, HUD_COLOR);
        }
    }

    private void renderFrame(GuiGraphics graphics, ClientDroneStore.ClientDrone drone) {
        int margin = 24;
        int right = width - margin;
        int bottom = height - margin;
        int length = 22;

        drawCorner(graphics, margin, margin, length, 1, 1);
        drawCorner(graphics, right, margin, length, -1, 1);
        drawCorner(graphics, margin, bottom, length, 1, -1);
        drawCorner(graphics, right, bottom, length, -1, -1);

        if ((drone.age() / 10) % 2 == 0) {
            graphics.fill(margin + 4, margin - 12, margin + 10, margin - 6, WARN_COLOR);
        }
        graphics.drawString(font, Component.literal("REC"), margin + 14, margin - 13, WARN_COLOR, false);
        graphics.drawString(font, Component.translatable(drone.kind().getTranslationKey())
                .withStyle(ChatFormatting.BOLD), margin, bottom + 6, HUD_COLOR, false);
    }

    private void drawCorner(GuiGraphics graphics, int x, int y, int length, int dirX, int dirY) {
        int thickness = 2;
        int x1 = dirX > 0 ? x : x - length;
        int x2 = dirX > 0 ? x + length : x;
        graphics.fill(x1, y, x2, y + thickness, HUD_COLOR);
        int y1 = dirY > 0 ? y : y - length;
        int y2 = dirY > 0 ? y + length : y;
        graphics.fill(x, y1, x + thickness, y2, HUD_COLOR);
    }

    private void renderTelemetry(GuiGraphics graphics, ClientDroneStore.ClientDrone drone) {
        int x = 30;
        int y = 46;
        int step = 11;

        Vec3 position = drone.position();
        double speed = drone.speed() * 20.0D * 3.6D;
        boolean mine = ClientTelemetry.isFor(netId);
        BlockPos target = ClientTelemetry.target();
        double distance = position.distanceTo(Vec3.atCenterOf(target));
        String metres = Component.translatable("screen.airsystem.feed.unit.metre").getString();
        String speedUnit = Component.translatable("screen.airsystem.feed.unit.speed").getString();
        int fuelPercent = mine
                ? Mth.clamp(ClientTelemetry.fuel() * 100 / AirSystemConfig.droneFlightTicks(), 0, 100)
                : 0;

        line(graphics, x, y, "screen.airsystem.feed.state",
                Component.translatable("state.airsystem." + drone.state().name().toLowerCase()).getString());
        line(graphics, x, y + step, "screen.airsystem.feed.altitude", "%.0f %s".formatted(position.y, metres));
        line(graphics, x, y + step * 2, "screen.airsystem.feed.speed", "%.0f %s".formatted(speed, speedUnit));
        line(graphics, x, y + step * 3, "screen.airsystem.feed.position",
                "%.0f / %.0f".formatted(position.x, position.z));
        line(graphics, x, y + step * 4, "screen.airsystem.feed.target",
                mine && ClientTelemetry.hasTarget()
                        ? target.getX() + " " + target.getY() + " " + target.getZ() : "—");
        line(graphics, x, y + step * 5, "screen.airsystem.feed.distance",
                mine && ClientTelemetry.hasTarget() ? "%.0f %s".formatted(distance, metres) : "—");
        line(graphics, x, y + step * 6, "screen.airsystem.feed.fuel", mine ? fuelPercent + " %" : "—");
        if (mine && drone.kind().getRole() == DroneKind.Role.STRIKE) {
            line(graphics, x, y + step * 7, "screen.airsystem.feed.munitions",
                    String.valueOf(ClientTelemetry.munitions()));
        }

        if (drone.state().isRecovering()) {
            graphics.drawString(font, Component.translatable("screen.airsystem.feed.returning")
                    .withStyle(ChatFormatting.BOLD), x, y + step * 8, HUD_COLOR, true);
        }
        if (drone.engineDead()) {
            graphics.drawString(font, Component.translatable("screen.airsystem.feed.engine_lost")
                    .withStyle(ChatFormatting.BOLD), x, y + step * 9, WARN_COLOR, true);
        }
        if (mine && ClientTelemetry.hitsLeft() < drone.kind().getCoreHits()) {
            graphics.drawString(font, Component.translatable("screen.airsystem.feed.damage",
                    ClientTelemetry.hitsLeft()), x, y + step * 10, WARN_COLOR, true);
        }
    }

    private void line(GuiGraphics graphics, int x, int y, String key, String value) {
        graphics.drawString(font, Component.translatable(key), x, y, HUD_DIM, false);
        graphics.drawString(font, Component.literal(value), x + 92, y, HUD_COLOR, false);
    }

    private void renderHeading(GuiGraphics graphics, ClientDroneStore.ClientDrone drone) {
        int centerX = width / 2;
        int y = 60;
        int halfWidth = 110;

        graphics.fill(centerX - halfWidth, y, centerX + halfWidth, y + 1, HUD_DIM);
        float heading = Mth.wrapDegrees(drone.lerpYaw(1.0F));

        for (int degrees = -180; degrees < 180; degrees += 15) {
            float offset = Mth.degreesDifference(heading, degrees);
            if (Math.abs(offset) > 60.0F) {
                continue;
            }
            int x = centerX + (int) (offset / 60.0F * halfWidth);
            boolean major = degrees % 45 == 0;
            graphics.fill(x, y - (major ? 5 : 3), x + 1, y, major ? HUD_COLOR : HUD_DIM);
            if (major) {
                graphics.drawCenteredString(font, Component.literal(String.valueOf((degrees + 360) % 360)),
                        x, y + 3, HUD_DIM);
            }
        }
        graphics.fill(centerX - 1, y - 8, centerX + 1, y + 1, WARN_COLOR);
    }

    private void renderReticle(GuiGraphics graphics) {
        int cx = width / 2;
        int cy = height / 2;
        int gap = 6;
        int arm = 10;

        graphics.fill(cx - gap - arm, cy, cx - gap, cy + 1, HUD_COLOR);
        graphics.fill(cx + gap, cy, cx + gap + arm, cy + 1, HUD_COLOR);
        graphics.fill(cx, cy - gap - arm, cx + 1, cy - gap, HUD_COLOR);
        graphics.fill(cx, cy + gap, cx + 1, cy + gap + arm, HUD_COLOR);
        graphics.fill(cx, cy, cx + 1, cy + 1, WARN_COLOR);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return isControlKey(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return isControlKey(keyCode) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    private static boolean isControlKey(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_W || keyCode == GLFW.GLFW_KEY_A
                || keyCode == GLFW.GLFW_KEY_S || keyCode == GLFW.GLFW_KEY_D
                || keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_LEFT_SHIFT;
    }

    @Override
    public void onClose() {
        command(DroneCommandPayload.CLOSE_FEED);
        super.onClose();
    }
}
