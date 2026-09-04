package com.rbxlu.airsystem.test;

import com.rbxlu.airsystem.AirSystem;
import com.rbxlu.airsystem.content.drone.DroneBlockEntity;
import com.rbxlu.airsystem.content.drone.DroneFlight;
import com.rbxlu.airsystem.content.drone.DroneFlightManager;
import com.rbxlu.airsystem.content.drone.DroneKind;
import com.rbxlu.airsystem.content.drone.DroneState;
import com.rbxlu.airsystem.content.drone.HitZone;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

// Run with: gradle runGameTestServer
@GameTestHolder(AirSystem.MODID)
@PrefixGameTestTemplate(false)
public class DroneFlightTests {
    private static final String ARENA = "empty";

    private static DroneFlight flightAbove(GameTestHelper helper, DroneKind kind, BlockPos target) {
        Vec3 start = Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)).add(0.0D, 120.0D, 0.0D);
        DroneFlight flight = new DroneFlight(UUID.randomUUID(), 1, kind, start, 0.0F);
        flight.setTarget(target);
        flight.setCruiseAltitude((int) start.y);
        flight.setState(DroneState.CRUISE);
        return flight;
    }

    private static BlockPos targetFor(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        return origin.offset(600, 0, 0);
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void flightApproachesTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DroneFlightManager manager = DroneFlightManager.get(level);

        BlockPos target = targetFor(helper);
        DroneFlight flight = flightAbove(helper, DroneKind.SHAHED_136, target);

        double before = flight.position().multiply(1.0D, 0.0D, 1.0D)
                .distanceTo(Vec3.atCenterOf(target).multiply(1.0D, 0.0D, 1.0D));

        for (int i = 0; i < 140 && flight.isAlive(); i++) {
            flight.tick(level, manager);
        }

        double after = flight.position().multiply(1.0D, 0.0D, 1.0D)
                .distanceTo(Vec3.atCenterOf(target).multiply(1.0D, 0.0D, 1.0D));

        helper.assertTrue(flight.isAlive(), "drone must survive an unobstructed flight");
        helper.assertTrue(after < before - 60.0D,
                "drone must close on the target: was %.1f, now %.1f".formatted(before, after));
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void coreHitsDestroyFlight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DroneFlightManager manager = DroneFlightManager.get(level);

        DroneFlight flight = flightAbove(helper, DroneKind.SHAHED_136, targetFor(helper));
        flight.tick(level, manager);

        int required = flight.kind().getCoreHits();
        for (int i = 0; i < required; i++) {
            helper.assertTrue(flight.isAlive(), "drone died before taking " + required + " hits");
            flight.hit(level, flight.position(), true, null);
        }

        helper.assertTrue(!flight.isAlive(), "drone must be destroyed after " + required + " centre hits");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void engineFailureMakesItFall(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DroneFlightManager manager = DroneFlightManager.get(level);

        DroneFlight flight = flightAbove(helper, DroneKind.ORLAN_10, targetFor(helper));
        flight.tick(level, manager);
        double altitude = flight.position().y;

        flight.failEngine(level, null);
        helper.assertTrue(flight.state() == DroneState.FALLING,
                "engine failure must put the drone into a stall");

        for (int i = 0; i < 20 && flight.isAlive(); i++) {
            flight.tick(level, manager);
        }

        helper.assertTrue(flight.position().y < altitude - 1.0D,
                "a downed drone must lose altitude");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void hitZonesAreDistinguished(GameTestHelper helper) {
        DroneFlight flight = flightAbove(helper, DroneKind.SHAHED_136, targetFor(helper));
        Vec3 centre = flight.position();
        DroneKind kind = flight.kind();

        helper.assertTrue(flight.classifyHit(centre) == HitZone.CORE,
                "a hit in the middle of the fuselage is a centre hit");
        helper.assertTrue(flight.classifyHit(centre.add(0.0D, 0.0D, -kind.getLength() * 0.45D)) == HitZone.ENGINE,
                "a hit in the tail is an engine hit");
        helper.assertTrue(flight.classifyHit(centre.add(kind.getWingspan() * 0.45D, 0.0D, 0.0D)) == HitZone.WING,
                "a hit on the panel is a wing hit");
        helper.assertTrue(flight.classifyHit(centre.add(kind.getWingspan() * 2.0D, 0.0D, 0.0D)) == HitZone.GRAZE,
                "outside the fuselage is a miss");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void reconReturnsAndLands(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DroneFlightManager manager = DroneFlightManager.get(level);

        BlockPos pad = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(pad.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pad, Blocks.AIR.defaultBlockState());

        Vec3 start = Vec3.atCenterOf(pad).add(0.0D, 3.0D, 0.0D);
        DroneFlight flight = new DroneFlight(UUID.randomUUID(), 1, DroneKind.ORLAN_10, start, 0.0F);
        flight.setHome(pad);
        flight.setState(DroneState.CRUISE);

        helper.assertTrue(flight.beginReturn(level), "a scout must accept the land command");
        helper.assertTrue(flight.state() == DroneState.RTB, "the command must put the drone into RTB");

        flight.setState(DroneState.LANDING);
        boolean finished = false;
        for (int i = 0; i < 240; i++) {
            if (flight.tick(level, manager)) {
                finished = true;
                break;
            }
        }

        helper.assertTrue(finished, "the drone must finish by landing, not loiter forever");
        helper.assertTrue(flight.state() == DroneState.LANDED,
                "a scout must land rather than crash: state " + flight.state());
        helper.assertTrue(level.getBlockEntity(pad) instanceof DroneBlockEntity stand
                        && stand.getKind() == DroneKind.ORLAN_10,
                "after landing the same drone stands on the pad again");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void kamikazeCannotLand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DroneFlight flight = flightAbove(helper, DroneKind.SHAHED_136, targetFor(helper));
        flight.setHome(helper.absolutePos(BlockPos.ZERO));

        helper.assertTrue(!flight.beginReturn(level), "a loitering munition does not land");
        helper.assertTrue(flight.state() == DroneState.CRUISE, "a refused landing must not change the flight phase");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void ownDronesAreNotEngaged(GameTestHelper helper) {
        Vec3 battery = Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO));

        DroneFlight own = flightAbove(helper, DroneKind.ORLAN_10, targetFor(helper));
        own.setHome(BlockPos.containing(battery).offset(20, 0, 0));

        DroneFlight hostile = flightAbove(helper, DroneKind.SHAHED_136, targetFor(helper));
        hostile.setHome(BlockPos.containing(battery).offset(400, 0, 0));

        helper.assertTrue(DroneFlightManager.isFriendly(own, battery, 70.0D),
                "a drone launched 20 blocks from the turret is friendly");
        helper.assertTrue(!DroneFlightManager.isFriendly(hostile, battery, 70.0D),
                "a drone launched 400 blocks away is hostile");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void kamikazeDivesOnTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        DroneFlightManager manager = DroneFlightManager.get(level);

        BlockPos target = helper.absolutePos(BlockPos.ZERO).offset(20, 0, 0);
        DroneFlight flight = flightAbove(helper, DroneKind.SHAHED_136, target);

        for (int i = 0; i < 10 && flight.isAlive(); i++) {
            flight.tick(level, manager);
        }

        helper.assertTrue(flight.state() == DroneState.DIVE || !flight.isAlive(),
                "over the target a loitering munition must dive, not circle");
        helper.succeed();
    }
}
