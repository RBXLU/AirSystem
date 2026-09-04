package com.rbxlu.airsystem.client.screen;

import com.rbxlu.airsystem.content.radar.RadarContact;
import com.rbxlu.airsystem.network.payload.RadarContactsPayload;
import com.rbxlu.airsystem.network.payload.RadarQueryPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class RadarScreen extends Screen {
    private static final int QUERY_INTERVAL = 10;
    private static final int PANEL_WIDTH = 168;

    private static final int BACKGROUND = 0xF00A0F0A;
    private static final int GRID = 0xFF1E4A28;
    private static final int GRID_BRIGHT = 0xFF2E6E3C;
    private static final int TEXT = 0xFF7CE08C;
    private static final int HOSTILE = 0xFFE05A46;
    private static final int FRIENDLY = 0xFF6ED0FF;

    private final BlockPos screenPos;

    private BlockPos origin = BlockPos.ZERO;
    private int range = 320;
    private int stations;
    private List<RadarContact> contacts = List.of();

    private int queryCooldown;
    private float sweep;

    public RadarScreen(BlockPos screenPos) {
        super(Component.translatable("screen.airsystem.radar.title"));
        this.screenPos = screenPos;
    }

    public void accept(RadarContactsPayload payload) {
        origin = payload.origin();
        range = payload.range();
        stations = payload.stations();
        contacts = payload.contacts();
    }

    @Override
    protected void init() {
        queryCooldown = 0;
    }

    @Override
    public void tick() {
        if (--queryCooldown <= 0) {
            queryCooldown = QUERY_INTERVAL;
            PacketDistributor.sendToServer(new RadarQueryPayload(screenPos));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        sweep += partialTick * 1.8F;
        if (sweep >= 360.0F) {
            sweep -= 360.0F;
        }

        int radius = Math.max(60, Math.min((width - PANEL_WIDTH - 60) / 2, height / 2 - 30));
        int cx = 24 + radius;
        int cy = height / 2;

        graphics.fill(0, 0, width, height, BACKGROUND);
        scope(graphics, cx, cy, radius);
        blips(graphics, cx, cy, radius);
        panel(graphics, width - PANEL_WIDTH - 12, Math.max(16, cy - radius));
    }

    private void scope(GuiGraphics graphics, int cx, int cy, int radius) {
        for (int ring = 1; ring <= 4; ring++) {
            circle(graphics, cx, cy, radius * ring / 4, ring == 4 ? GRID_BRIGHT : GRID);
        }
        graphics.fill(cx - radius, cy, cx + radius, cy + 1, GRID);
        graphics.fill(cx, cy - radius, cx + 1, cy + radius, GRID);

        // The sweep is a client-side flourish: contacts refresh on the server's own
        // schedule, so the line is not what reveals them.
        double angle = Math.toRadians(sweep);
        int tipX = cx + (int) (Math.sin(angle) * radius);
        int tipY = cy - (int) (Math.cos(angle) * radius);
        line(graphics, cx, cy, tipX, tipY, GRID_BRIGHT);

        graphics.drawString(font, "N", cx - 3, cy - radius - 11, TEXT, false);
        graphics.drawString(font, Component.translatable("screen.airsystem.radar.range", range),
                cx - radius, cy + radius + 6, TEXT, false);
    }

    private void blips(GuiGraphics graphics, int cx, int cy, int radius) {
        double scale = (double) radius / range;
        for (RadarContact contact : contacts) {
            double dx = contact.x() - (origin.getX() + 0.5D);
            double dz = contact.z() - (origin.getZ() + 0.5D);
            int bx = cx + (int) (dx * scale);
            int by = cy + (int) (dz * scale);
            if (Mth.square(bx - cx) + Mth.square(by - cy) > Mth.square(radius)) {
                continue;
            }
            int colour = contact.friendly() ? FRIENDLY : HOSTILE;
            graphics.fill(bx - 2, by - 2, bx + 3, by + 3, colour);
            graphics.fill(bx - 1, by - 1, bx + 2, by + 2, 0xFFFFFFFF);

            // Heading stalk: a contact's course is half of what makes a scope readable.
            double heading = Math.toRadians(contact.heading());
            int hx = bx + (int) (-Math.sin(heading) * 8.0D);
            int hy = by + (int) (Math.cos(heading) * 8.0D);
            line(graphics, bx, by, hx, hy, colour);
        }
    }

    private void panel(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, title.copy().withStyle(ChatFormatting.BOLD), x, y, TEXT, false);
        graphics.drawString(font, stations == 0
                        ? Component.translatable("message.airsystem.radar.no_station")
                        : Component.translatable("screen.airsystem.radar.stations", stations),
                x, y + 12, stations == 0 ? HOSTILE : GRID_BRIGHT, false);
        graphics.drawString(font, Component.translatable("screen.airsystem.radar.contacts", contacts.size()),
                x, y + 24, TEXT, false);

        int row = y + 42;
        for (RadarContact contact : contacts) {
            if (row > height - 30) {
                break;
            }
            double dx = contact.x() - (origin.getX() + 0.5D);
            double dz = contact.z() - (origin.getZ() + 0.5D);
            int bearing = Math.floorMod((int) Math.toDegrees(Math.atan2(dx, -dz)), 360);
            int distance = (int) Math.sqrt(dx * dx + dz * dz);

            Component name = contact.friendly()
                    ? Component.translatable(contact.droneKind().getTranslationKey())
                    : Component.translatable(contact.sizeClass().key());
            graphics.drawString(font, name, x, row, contact.friendly() ? FRIENDLY : HOSTILE, false);
            graphics.drawString(font, Component.translatable("screen.airsystem.radar.track",
                            bearing, distance, (int) contact.y()), x, row + 10, TEXT, false);
            graphics.drawString(font, Component.translatable("screen.airsystem.radar.speed",
                            (int) (contact.speed() * 72.0F)), x, row + 20, TEXT, false);
            row += 34;
        }

        if (contacts.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.airsystem.radar.clear"),
                    x, y + 42, GRID_BRIGHT, false);
        }
    }

    private static void circle(GuiGraphics graphics, int cx, int cy, int radius, int colour) {
        int steps = Math.max(48, radius * 2);
        for (int i = 0; i < steps; i++) {
            double angle = i * 2.0D * Math.PI / steps;
            int x = cx + (int) (Math.cos(angle) * radius);
            int y = cy + (int) (Math.sin(angle) * radius);
            graphics.fill(x, y, x + 1, y + 1, colour);
        }
    }

    private static void line(GuiGraphics graphics, int x1, int y1, int x2, int y2, int colour) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            float t = steps == 0 ? 0.0F : (float) i / steps;
            int x = Math.round(Mth.lerp(t, x1, x2));
            int y = Math.round(Mth.lerp(t, y1, y2));
            graphics.fill(x, y, x + 1, y + 1, colour);
        }
    }
}
