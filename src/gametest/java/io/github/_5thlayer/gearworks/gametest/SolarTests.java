// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.SuitFixtures.suitWith;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.wearer;

import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.gear.GearContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;

/**
 * Solar charges the worn suit in daylight under open sky. The test ticks its player only once the level
 * reads open sky at its eyes: sky light reaches the light engine some ticks after the chunk does, and on
 * a busy server, still lighting the chunks it just made, that has taken more than five. When solar
 * charges nothing, at night or under a roof, is checked by {@code SolarTest} on a plain JVM: a roof
 * here waited on the light engine to cast its shadow, which a busy server could stall for good (#8).
 */
final class SolarTests {

    private static final int MAX_TICKS = 100;

    private SolarTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        var clear = new TestEnvironmentDefinition.Weather(TestEnvironmentDefinition.Weather.Type.CLEAR);
        var day = tests.environment("noon", new DayTime(DayTime.NOON), clear);
        tests.test("solar_charges_in_daylight_under_open_sky", MAX_TICKS, day, SolarTests::chargesByDay);
    }

    private static void chargesByDay(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SOLAR_UPGRADE.get());
        var player = wearer(helper, suit);
        var ticking = new boolean[1];
        helper.onEachTick(() -> {
            if (ticking[0]) {
                player.tick();
            }
        });
        var level = helper.getLevel();
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(level.isBrightOutside(), Component.literal("not bright outside yet"));
                    helper.assertTrue(level.canSeeSky(BlockPos.containing(player.getEyePosition())),
                            Component.literal("no sky at the player's eyes yet"));
                })
                .thenExecute(() -> ticking[0] = true)
                .thenWaitUntil(() -> helper.assertTrue(Suit.energy(suit) > 0, Component.literal("no FE from the sun")))
                .thenSucceed();
    }
}
