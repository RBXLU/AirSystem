package com.rbxlu.airsystem.content.turret;

import com.rbxlu.airsystem.AirSystem;
import net.minecraft.resources.ResourceLocation;

public enum TurretKind {
    GEPARD("gepard", 3.6F, 3.4F, 3.0F, 96.0F, 2, 3, 0.85F, 320, "35mm", 0x5B6148, false),

    SLINGER("slinger", 2.4F, 2.2F, 2.2F, 72.0F, 3, 1, 0.80F, 200, "30mm", 0x6B6F5E, false),

    TERRAHAWK_PALADIN("terrahawk_paladin", 3.0F, 3.0F, 3.6F, 84.0F, 4, 1, 0.78F, 260, "30mm", 0x7A7468,
            false),

    MANTIS("mantis", 3.2F, 3.2F, 3.4F, 110.0F, 2, 1, 0.88F, 400, "35mm", 0x4A5348, true);

    private final String id;
    private final float width;
    private final float length;
    private final float height;
    private final float range;
    private final int fireInterval;
    private final int barrels;
    private final float accuracy;
    private final int magazine;
    private final String caliber;
    private final int tintColor;
    private final boolean autoOnly;

    TurretKind(String id, float width, float length, float height, float range, int fireInterval,
               int barrels, float accuracy, int magazine, String caliber, int tintColor,
               boolean autoOnly) {
        this.id = id;
        this.width = width;
        this.length = length;
        this.height = height;
        this.range = range;
        this.fireInterval = fireInterval;
        this.barrels = barrels;
        this.accuracy = accuracy;
        this.magazine = magazine;
        this.caliber = caliber;
        this.tintColor = tintColor;
        this.autoOnly = autoOnly;
    }

    public String getId() {
        return id;
    }

    public float getWidth() {
        return width;
    }

    public float getLength() {
        return length;
    }

    public float getHeight() {
        return height;
    }

    public float getRange() {
        return range;
    }

    public int getFireInterval() {
        return fireInterval;
    }

    public int getBarrels() {
        return barrels;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public int getMagazine() {
        return magazine;
    }

    public String getCaliber() {
        return caliber;
    }

    public int getTintColor() {
        return tintColor;
    }

    public boolean isAutoOnly() {
        return autoOnly;
    }

    public String getTranslationKey() {
        return "entity." + AirSystem.MODID + "." + id;
    }

    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "textures/entity/turret/" + id + ".png");
    }

    public static TurretKind byId(String id) {
        for (TurretKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return GEPARD;
    }
}
