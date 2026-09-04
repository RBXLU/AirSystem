package com.rbxlu.airsystem.content.drone;

import com.rbxlu.airsystem.AirSystem;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

// Units: speed in blocks per tick (20 ticks = 1 s), length and span in blocks,
// ceiling as a Y coordinate.
public enum DroneKind {
    SHAHED_131("shahed_131", Role.KAMIKAZE, 3.5F, 3.0F, 1.05F, 0.030F, 220,
            3, 5.0F, false, 30, EngineSound.PISTON, 0xB4A67C),
    SHAHED_136("shahed_136", Role.KAMIKAZE, 4.2F, 3.0F, 1.20F, 0.028F, 240,
            3, 6.5F, true, 35, EngineSound.PISTON, 0x8E8B72),
    SHAHED_238("shahed_238", Role.KAMIKAZE, 4.2F, 3.0F, 2.60F, 0.022F, 300,
            3, 7.0F, true, 40, EngineSound.JET, 0x3C3F44),

    ORLAN_10("orlan_10", Role.RECON, 2.0F, 3.1F, 0.95F, 0.045F, 260,
            2, 0.0F, false, 0, EngineSound.PISTON, 0xDCDCD2),
    ORLAN_30("orlan_30", Role.RECON, 2.4F, 3.4F, 1.05F, 0.045F, 300,
            2, 0.0F, false, 0, EngineSound.PISTON, 0xC8C8BE),
    ELERON_3("eleron_3", Role.RECON, 1.4F, 2.2F, 0.90F, 0.055F, 200,
            2, 0.0F, false, 0, EngineSound.ELECTRIC, 0x6E7A5A),
    ZALA_421_16E("zala_421_16e", Role.RECON, 1.8F, 2.8F, 0.95F, 0.050F, 240,
            2, 0.0F, false, 0, EngineSound.ELECTRIC, 0xE0E0D8),
    ZALA_421_08("zala_421_08", Role.RECON, 1.0F, 1.6F, 0.85F, 0.065F, 180,
            1, 0.0F, false, 0, EngineSound.ELECTRIC, 0xD2D2C8),
    GRANAT_4("granat_4", Role.RECON, 2.2F, 3.2F, 1.00F, 0.045F, 280,
            2, 0.0F, false, 0, EngineSound.PISTON, 0xA8AC96),

    LANCET_1("lancet_1", Role.KAMIKAZE, 1.1F, 1.0F, 1.30F, 0.060F, 180,
            2, 3.0F, false, 18, EngineSound.ELECTRIC, 0x4E5B44),
    LANCET_3("lancet_3", Role.KAMIKAZE, 1.6F, 1.4F, 1.40F, 0.058F, 200,
            2, 4.0F, true, 24, EngineSound.ELECTRIC, 0x44503C),
    KUB_BLA("kub_bla", Role.KAMIKAZE, 1.3F, 1.5F, 1.25F, 0.055F, 180,
            2, 3.5F, false, 20, EngineSound.ELECTRIC, 0x5A5F52),
    ORION("orion", Role.STRIKE, 5.0F, 8.0F, 1.10F, 0.035F, 300,
            4, 0.0F, false, 0, EngineSound.PISTON, 0xB0B4A4),
    S_70("s_70", Role.STRIKE, 7.0F, 9.0F, 2.80F, 0.025F, 320,
            5, 0.0F, false, 0, EngineSound.JET, 0x2E3238),

    LELEKA_100("leleka_100", Role.RECON, 1.6F, 2.0F, 0.95F, 0.055F, 220,
            2, 0.0F, false, 0, EngineSound.ELECTRIC, 0xC0C4B4),
    SHARK("shark", Role.RECON, 2.2F, 3.0F, 1.05F, 0.050F, 260,
            2, 0.0F, false, 0, EngineSound.ELECTRIC, 0xA6AA9A),
    PD_2("pd_2", Role.RECON, 2.6F, 3.6F, 1.00F, 0.045F, 280,
            3, 0.0F, false, 0, EngineSound.PISTON, 0xD8D8D0),
    LIUTYI("liutyi", Role.KAMIKAZE, 4.4F, 4.2F, 1.30F, 0.030F, 260,
            3, 6.0F, true, 34, EngineSound.PISTON, 0x2A2E33),
    UJ_22("uj_22", Role.KAMIKAZE, 3.4F, 4.0F, 1.25F, 0.035F, 240,
            3, 5.0F, false, 30, EngineSound.PISTON, 0x5C6154),
    RAM_II("ram_ii", Role.KAMIKAZE, 1.7F, 2.0F, 1.35F, 0.055F, 200,
            2, 3.5F, true, 22, EngineSound.ELECTRIC, 0x707A62);

    public enum Role {
        RECON,

        KAMIKAZE,

        STRIKE;

        public boolean canRecover() {
            return this != KAMIKAZE;
        }
    }

    public enum EngineSound {
        PISTON,
        JET,
        ELECTRIC
    }

    private final String id;
    private final Role role;
    private final float length;
    private final float wingspan;
    private final float cruiseSpeed;
    private final float turnRate;
    private final int ceiling;
    private final int coreHits;
    private final float warheadPower;
    private final boolean incendiary;
    private final int windowBreakRadius;
    private final EngineSound engineSound;
    private final int tintColor;

    DroneKind(String id, Role role, float length, float wingspan, float cruiseSpeed, float turnRate,
              int ceiling, int coreHits, float warheadPower, boolean incendiary, int windowBreakRadius,
              EngineSound engineSound, int tintColor) {
        this.id = id;
        this.role = role;
        this.length = length;
        this.wingspan = wingspan;
        this.cruiseSpeed = cruiseSpeed;
        this.turnRate = turnRate;
        this.ceiling = ceiling;
        this.coreHits = coreHits;
        this.warheadPower = warheadPower;
        this.incendiary = incendiary;
        this.windowBreakRadius = windowBreakRadius;
        this.engineSound = engineSound;
        this.tintColor = tintColor;
    }

    public String getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public float getLength() {
        return length;
    }

    public float getWingspan() {
        return wingspan;
    }

    public float getCruiseSpeed() {
        return cruiseSpeed;
    }

    public float getTurnRate() {
        return turnRate;
    }

    public int getCeiling() {
        return ceiling;
    }

    public int getCoreHits() {
        return coreHits;
    }

    public float getWarheadPower() {
        return warheadPower;
    }

    public boolean isIncendiary() {
        return incendiary;
    }

    public int getWindowBreakRadius() {
        return windowBreakRadius;
    }

    public EngineSound getEngineSound() {
        return engineSound;
    }

    public int getTintColor() {
        return tintColor;
    }

    public boolean hasWarhead() {
        return warheadPower > 0.0F;
    }

    public boolean canRecover() {
        return role.canRecover();
    }

    public float getApproachSpeed() {
        return Math.max(0.35F, cruiseSpeed * 0.5F);
    }

    public String getTranslationKey() {
        return "entity." + AirSystem.MODID + "." + id;
    }

    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(AirSystem.MODID, "textures/entity/drone/" + id + ".png");
    }

    public static DroneKind byId(String id) {
        for (DroneKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return SHAHED_136;
    }

    public static DroneKind byOrdinal(int ordinal) {
        DroneKind[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }

    public String serialize() {
        return name().toLowerCase(Locale.ROOT);
    }
}
