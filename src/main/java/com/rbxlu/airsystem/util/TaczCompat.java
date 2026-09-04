package com.rbxlu.airsystem.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.util.Locale;

// Soft TACZ support: matched by item and class id, never compiled against.
public final class TaczCompat {
    public static final String TACZ_ID = "tacz";

    private static Boolean loaded;

    public static boolean isTaczLoaded() {
        if (loaded == null) {
            loaded = ModList.get() != null && ModList.get().isLoaded(TACZ_ID);
        }
        return loaded;
    }

    public static boolean isGunProjectile(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        String className = entity.getClass().getName();
        if (className.startsWith("com.tacz.guns")) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String path = id.getPath().toLowerCase(Locale.ROOT);
        return TACZ_ID.equals(id.getNamespace())
                || path.contains("bullet")
                || path.contains("projectile");
    }

    public static boolean isAmmoFor(ItemStack stack, String caliber) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath().toLowerCase(Locale.ROOT);

        if (id.getNamespace().equals(com.rbxlu.airsystem.AirSystem.MODID)) {
            return path.contains(caliber.toLowerCase(Locale.ROOT));
        }

        if (id.getNamespace().equals(TACZ_ID)) {
            return path.contains("ammo") || path.contains(caliber.toLowerCase(Locale.ROOT));
        }
        return false;
    }

    public static int roundsPerItem(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals(TACZ_ID) ? 10 : 20;
    }

    private TaczCompat() {
    }
}
