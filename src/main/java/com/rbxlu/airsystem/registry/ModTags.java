package com.rbxlu.airsystem.registry;

import com.rbxlu.airsystem.AirSystem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final class Blocks {
        public static final TagKey<Block> SHOCKWAVE_FRAGILE = create("shockwave_fragile");

        private static TagKey<Block> create(String name) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, name));
        }

        private Blocks() {
        }
    }

    private ModTags() {
    }
}
