// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.GearFixtures.suitWith;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.wearer;

import io.github._5thlayer.gearworks.gear.Gear;
import io.github._5thlayer.gearworks.gear.GearContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Solar charges the worn suit in daylight under open sky, and not at night or under a roof. */
final class SolarTests {

    private static final int WAIT = 20;
    private static final int SETTLE = 5;

    private SolarTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        var clear = new TestEnvironmentDefinition.Weather(TestEnvironmentDefinition.Weather.Type.CLEAR);
        var day = tests.environment("noon", new DayTime(DayTime.NOON), clear);
        var night = tests.environment("midnight", new DayTime(DayTime.MIDNIGHT), clear);
        tests.test("solar_charges_in_daylight_under_open_sky", 40, day, SolarTests::chargesByDay);
        tests.test("solar_does_not_charge_under_a_roof", 40, day, SolarTests::shaded);
        tests.test("solar_does_not_charge_at_night", 40, night, SolarTests::idleAtNight);
    }

    private static void chargesByDay(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SOLAR_UPGRADE.get());
        var player = wearer(helper, suit);
        helper.onEachTick(player::tick);
        helper.succeedWhen(() -> helper.assertTrue(Gear.energy(suit) > 0, Component.literal("no FE from the sun")));
    }

    private static void shaded(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SOLAR_UPGRADE.get());
        var player = wearer(helper, suit);
        helper.setBlock(new BlockPos(1, 4, 1), Blocks.STONE);
        // Sky light reaches the light engine a tick or so after the roof does.
        helper.onEachTick(() -> {
            if (helper.getTick() > SETTLE) {
                player.tick();
            }
        });
        helper.runAfterDelay(SETTLE + WAIT, () -> {
            helper.assertValueEqual(Gear.energy(suit), 0, Component.literal("FE held"));
            helper.succeed();
        });
    }

    private static void idleAtNight(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SOLAR_UPGRADE.get());
        var player = wearer(helper, suit);
        helper.onEachTick(player::tick);
        noFeAfterAWhile(helper, suit);
    }

    private static void noFeAfterAWhile(GameTestHelper helper, ItemStack suit) {
        helper.runAfterDelay(WAIT, () -> {
            helper.assertValueEqual(Gear.energy(suit), 0, Component.literal("FE held"));
            helper.succeed();
        });
    }
}
