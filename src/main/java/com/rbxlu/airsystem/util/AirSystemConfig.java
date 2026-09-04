package com.rbxlu.airsystem.util;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AirSystemConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue DRONE_FLIGHT_TICKS = BUILDER
            .comment("Ticks a drone stays airborne before it runs out of fuel")
            .defineInRange("drone.flightTicks", 12000, 600, 432000);

    private static final ModConfigSpec.IntValue CRUISE_ALTITUDE = BUILDER
            .comment("How many blocks above the target a drone cruises")
            .defineInRange("drone.cruiseAltitude", 70, 10, 300);

    private static final ModConfigSpec.DoubleValue ENGINE_FAILURE_CHANCE = BUILDER
            .comment("Chance a hit knocks the engine out")
            .defineInRange("drone.engineFailureChance", 0.20D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue CONTROL_RANGE = BUILDER
            .comment("Remote control link range, in blocks")
            .defineInRange("drone.controlRange", 4000.0D, 64.0D, 100000.0D);

    private static final ModConfigSpec.IntValue CHUNK_LOAD_RADIUS = BUILDER
            .comment("Radius of force-loaded chunks around a drone, in chunks")
            .defineInRange("drone.chunkLoadRadius", 2, 1, 8);

    private static final ModConfigSpec.DoubleValue SYNC_RANGE = BUILDER
            .comment("Distance at which a player is sent nearby drones, in blocks")
            .defineInRange("drone.syncRange", 320.0D, 64.0D, 2048.0D);

    private static final ModConfigSpec.IntValue MAX_FLIGHTS = BUILDER
            .comment("Maximum drones airborne at once per dimension")
            .defineInRange("drone.maxFlights", 64, 1, 512);

    private static final ModConfigSpec.DoubleValue SHAKE_RADIUS = BUILDER
            .comment("Radius within which players feel the screen shake")
            .defineInRange("impact.shakeRadius", 90.0D, 8.0D, 512.0D);

    private static final ModConfigSpec.DoubleValue DISTANT_SOUND_RADIUS = BUILDER
            .comment("Radius within which a distant detonation is heard")
            .defineInRange("impact.distantSoundRadius", 320.0D, 32.0D, 2048.0D);

    private static final ModConfigSpec.IntValue SHOCKWAVE_TICKS = BUILDER
            .comment("Ticks the blast wave takes to reach the edge of its radius")
            .defineInRange("impact.shockwaveTicks", 16, 1, 200);

    private static final ModConfigSpec.BooleanValue BREAK_WINDOWS = BUILDER
            .comment("Whether the blast wave breaks glass")
            .define("impact.breakWindows", true);

    private static final ModConfigSpec.BooleanValue START_FIRES = BUILDER
            .comment("Whether incendiary warheads set the ground on fire")
            .define("impact.startFires", true);

    private static final ModConfigSpec.DoubleValue FRIENDLY_RADIUS = BUILDER
            .comment("IFF radius: drones launched closer than this to a turret are treated as"
                    + " friendly and never engaged automatically")
            .defineInRange("turret.friendlyRadius", 70.0D, 0.0D, 512.0D);

    private static final ModConfigSpec.DoubleValue AUTO_HIT_CHANCE = BUILDER
            .comment("Base chance a burst hits in automatic mode; falls off with range and"
                    + " target speed")
            .defineInRange("turret.autoHitChance", 0.55D, 0.02D, 1.0D);

    private static final ModConfigSpec.IntValue SIREN_RADIUS = BUILDER
            .comment("Air raid siren audible radius, in blocks")
            .defineInRange("alarm.sirenRadius", 160, 16, 512);

    private static final ModConfigSpec.IntValue ALERT_DURATION = BUILDER
            .comment("Seconds an alert sounds before the siren stops by itself."
                    + " 0 means it sounds until all-clear or a second press")
            .defineInRange("alarm.alertDuration", 90, 0, 3600);

    private static final ModConfigSpec.IntValue RADAR_RANGE = BUILDER
            .comment("Radar station detection range, in blocks")
            .defineInRange("radar.range", 320, 64, 2048);

    private static final ModConfigSpec.IntValue RADAR_SWEEP = BUILDER
            .comment("Ticks between radar sweeps; contacts on the scope refresh this often")
            .defineInRange("radar.sweepTicks", 20, 4, 200);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static final double SOUND_BLOCKS_PER_TICK = 17.0D;

    private static boolean loaded;

    public static void markLoaded() {
        loaded = true;
    }

    public static int droneFlightTicks() {
        return loaded ? DRONE_FLIGHT_TICKS.get() : 12000;
    }

    public static int cruiseAltitude() {
        return loaded ? CRUISE_ALTITUDE.get() : 70;
    }

    public static float engineFailureChance() {
        return (float) (loaded ? ENGINE_FAILURE_CHANCE.get() : 0.20D);
    }

    public static double controlRange() {
        return loaded ? CONTROL_RANGE.get() : 4000.0D;
    }

    public static int chunkLoadRadius() {
        return loaded ? CHUNK_LOAD_RADIUS.get() : 2;
    }

    public static double syncRange() {
        return loaded ? SYNC_RANGE.get() : 320.0D;
    }

    public static int maxFlights() {
        return loaded ? MAX_FLIGHTS.get() : 64;
    }

    public static double shakeRadius() {
        return loaded ? SHAKE_RADIUS.get() : 90.0D;
    }

    public static double distantSoundRadius() {
        return loaded ? DISTANT_SOUND_RADIUS.get() : 320.0D;
    }

    public static int shockwaveTicks() {
        return loaded ? SHOCKWAVE_TICKS.get() : 16;
    }

    public static boolean breakWindows() {
        return !loaded || BREAK_WINDOWS.get();
    }

    public static boolean startFires() {
        return !loaded || START_FIRES.get();
    }

    public static double friendlyRadius() {
        return loaded ? FRIENDLY_RADIUS.get() : 70.0D;
    }

    public static float autoHitChance() {
        return (float) (loaded ? AUTO_HIT_CHANCE.get() : 0.55D);
    }

    public static int sirenRadius() {
        return loaded ? SIREN_RADIUS.get() : 160;
    }

    public static int alertDurationTicks() {
        return (loaded ? ALERT_DURATION.get() : 90) * 20;
    }

    public static int radarRange() {
        return loaded ? RADAR_RANGE.get() : 320;
    }

    public static int radarSweepTicks() {
        return loaded ? RADAR_SWEEP.get() : 20;
    }

    private AirSystemConfig() {
    }
}
