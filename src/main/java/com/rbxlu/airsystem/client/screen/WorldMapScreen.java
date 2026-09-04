package com.rbxlu.airsystem.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.network.payload.RemoteActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class WorldMapScreen extends Screen {
    private static final int MAP_PIXELS = 192;
    private static final int[] ZOOM_LEVELS = {1, 2, 4, 8, 16};

    private final InteractionHand hand;

    private DynamicTexture texture;
    private ResourceLocation textureId;

    private int centerX;
    private int centerZ;
    private int zoomIndex = 1;

    @Nullable
    private BlockPos marker;
    private boolean dragging;
    private double dragStartX;
    private double dragStartZ;
    private double dragMouseX;
    private double dragMouseY;
    private long copiedAt;
    private boolean needsRebuild;

    public WorldMapScreen(InteractionHand hand) {
        super(Component.translatable("screen.airsystem.world_map"));
        this.hand = hand;
    }

    private int blocksPerPixel() {
        return ZOOM_LEVELS[Mth.clamp(zoomIndex, 0, ZOOM_LEVELS.length - 1)];
    }

    private int mapLeft() {
        return (width - MAP_PIXELS) / 2;
    }

    private int mapTop() {
        return (height - MAP_PIXELS) / 2 - 10;
    }

    @Override
    protected void init() {
        if (minecraft != null && minecraft.player != null) {
            centerX = minecraft.player.blockPosition().getX();
            centerZ = minecraft.player.blockPosition().getZ();
        }

        texture = new DynamicTexture(MAP_PIXELS, MAP_PIXELS, false);
        textureId = minecraft.getTextureManager().register("airsystem_world_map", texture);
        rebuildMap();

        int bottom = mapTop() + MAP_PIXELS + 8;
        addRenderableWidget(Button.builder(Component.translatable("screen.airsystem.map.copy"),
                        button -> copyMarker())
                .bounds(mapLeft(), bottom, 96, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.airsystem.map.center"),
                        button -> {
                            if (minecraft != null && minecraft.player != null) {
                                centerX = minecraft.player.blockPosition().getX();
                                centerZ = minecraft.player.blockPosition().getZ();
                                rebuildMap();
                            }
                        })
                .bounds(mapLeft() + 100, bottom, 92, 20).build());
    }

    private void rebuildMap() {
        if (minecraft == null || minecraft.level == null || texture == null) {
            return;
        }
        NativeImage image = texture.getPixels();
        if (image == null) {
            return;
        }

        int scale = blocksPerPixel();
        int half = MAP_PIXELS / 2;
        var level = minecraft.level;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int px = 0; px < MAP_PIXELS; px++) {
            for (int py = 0; py < MAP_PIXELS; py++) {
                int worldX = centerX + (px - half) * scale;
                int worldZ = centerZ + (py - half) * scale;

                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                if (surface <= level.getMinBuildHeight() + 1) {
                    image.setPixelRGBA(px, py, 0xFF201F1E);
                    continue;
                }

                pos.set(worldX, surface - 1, worldZ);
                MapColor mapColor = level.getBlockState(pos).getMapColor(level, pos);
                if (mapColor == MapColor.NONE) {
                    image.setPixelRGBA(px, py, 0xFF201F1E);
                    continue;
                }

                int northHeight = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ - scale);
                MapColor.Brightness brightness;
                if (surface > northHeight) {
                    brightness = MapColor.Brightness.HIGH;
                } else if (surface < northHeight) {
                    brightness = MapColor.Brightness.LOW;
                } else {
                    brightness = MapColor.Brightness.NORMAL;
                }
                image.setPixelRGBA(px, py, mapColor.calculateRGBColor(brightness));
            }
        }
        texture.upload();
    }

    @Override
    public void tick() {
        super.tick();
        if (needsRebuild) {
            needsRebuild = false;
            rebuildMap();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int left = mapLeft();
        int top = mapTop();

        graphics.fill(left - 3, top - 3, left + MAP_PIXELS + 3, top + MAP_PIXELS + 3, 0xFF2B2B2B);
        graphics.fill(left - 1, top - 1, left + MAP_PIXELS + 1, top + MAP_PIXELS + 1, 0xFF101010);
        if (textureId != null) {
            graphics.blit(textureId, left, top, 0.0F, 0.0F, MAP_PIXELS, MAP_PIXELS, MAP_PIXELS, MAP_PIXELS);
        }

        drawGrid(graphics, left, top);
        drawPlayerMarker(graphics, left, top);
        drawTargetMarker(graphics, left, top);

        graphics.drawCenteredString(font, title, width / 2, top - 26, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("screen.airsystem.map.scale", blocksPerPixel()),
                width / 2, top - 14, 0xFFAAAAAA);

        BlockPos hovered = toWorld(mouseX, mouseY);
        if (hovered != null) {
            graphics.drawCenteredString(font,
                    Component.literal(hovered.getX() + " " + hovered.getY() + " " + hovered.getZ()),
                    width / 2, top + MAP_PIXELS + 34, 0xFFDDDDDD);
        }

        if (marker != null) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.airsystem.map.marked",
                            marker.getX() + " " + marker.getY() + " " + marker.getZ())
                            .withStyle(ChatFormatting.GOLD),
                    width / 2, top + MAP_PIXELS + 46, 0xFFFFAA00);
        }

        if (System.currentTimeMillis() - copiedAt < 2000L) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.airsystem.map.copied").withStyle(ChatFormatting.GREEN),
                    width / 2, top + MAP_PIXELS + 58, 0xFF55FF55);
        } else {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.airsystem.map.hint").withStyle(ChatFormatting.DARK_GRAY),
                    width / 2, top + MAP_PIXELS + 58, 0xFF888888);
        }
    }

    private void drawGrid(GuiGraphics graphics, int left, int top) {
        int scale = blocksPerPixel();
        int step = 64 / scale;
        if (step < 8) {
            step = 16;
        }
        for (int offset = step; offset < MAP_PIXELS; offset += step) {
            graphics.fill(left + offset, top, left + offset + 1, top + MAP_PIXELS, 0x22FFFFFF);
            graphics.fill(left, top + offset, left + MAP_PIXELS, top + offset + 1, 0x22FFFFFF);
        }
    }

    private void drawPlayerMarker(GuiGraphics graphics, int left, int top) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        int scale = blocksPerPixel();
        int px = left + MAP_PIXELS / 2 + (minecraft.player.blockPosition().getX() - centerX) / scale;
        int py = top + MAP_PIXELS / 2 + (minecraft.player.blockPosition().getZ() - centerZ) / scale;
        if (px < left || px > left + MAP_PIXELS || py < top || py > top + MAP_PIXELS) {
            return;
        }
        graphics.fill(px - 2, py - 2, px + 3, py + 3, 0xFFFFFFFF);
        graphics.fill(px - 1, py - 1, px + 2, py + 2, 0xFF2277FF);
    }

    private void drawTargetMarker(GuiGraphics graphics, int left, int top) {
        if (marker == null) {
            return;
        }
        int scale = blocksPerPixel();
        int px = left + MAP_PIXELS / 2 + (marker.getX() - centerX) / scale;
        int py = top + MAP_PIXELS / 2 + (marker.getZ() - centerZ) / scale;
        if (px < left || px > left + MAP_PIXELS || py < top || py > top + MAP_PIXELS) {
            return;
        }
        graphics.fill(px - 5, py, px + 6, py + 1, 0xFFFF3030);
        graphics.fill(px, py - 5, px + 1, py + 6, 0xFFFF3030);
        graphics.fill(px - 2, py - 2, px + 3, py + 3, 0x60FF3030);
    }

    @Nullable
    private BlockPos toWorld(double mouseX, double mouseY) {
        int left = mapLeft();
        int top = mapTop();
        if (mouseX < left || mouseX >= left + MAP_PIXELS || mouseY < top || mouseY >= top + MAP_PIXELS) {
            return null;
        }
        if (minecraft == null || minecraft.level == null) {
            return null;
        }

        int scale = blocksPerPixel();
        int worldX = centerX + ((int) mouseX - left - MAP_PIXELS / 2) * scale;
        int worldZ = centerZ + ((int) mouseY - top - MAP_PIXELS / 2) * scale;
        int worldY = minecraft.level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
        return new BlockPos(worldX, worldY, worldZ);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            BlockPos clicked = toWorld(mouseX, mouseY);
            if (clicked != null) {
                marker = clicked;
                PacketDistributor.sendToServer(new RemoteActionPayload(
                        RemoteActionPayload.MARK_MAP, clicked, hand.name()));
                copyMarker();
                return true;
            }
        }
        if (button == 1) {
            dragging = true;
            dragMouseX = mouseX;
            dragMouseY = mouseY;
            dragStartX = centerX;
            dragStartZ = centerZ;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 1) {
            int scale = blocksPerPixel();
            centerX = (int) (dragStartX - (mouseX - dragMouseX) * scale);
            centerZ = (int) (dragStartZ - (mouseY - dragMouseY) * scale);

            needsRebuild = true;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int previous = zoomIndex;
        zoomIndex = Mth.clamp(zoomIndex - (int) Math.signum(scrollY), 0, ZOOM_LEVELS.length - 1);
        if (previous != zoomIndex) {
            needsRebuild = true;
        }
        return true;
    }

    private void copyMarker() {
        if (marker == null || minecraft == null) {
            return;
        }
        minecraft.keyboardHandler.setClipboard(marker.getX() + " " + marker.getY() + " " + marker.getZ());
        copiedAt = System.currentTimeMillis();
    }

    @Override
    public void removed() {
        if (textureId != null && minecraft != null) {
            minecraft.getTextureManager().release(textureId);
        }
        if (texture != null) {
            texture.close();
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static ResourceLocation icon() {
        return ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "textures/item/world_map.png");
    }
}
