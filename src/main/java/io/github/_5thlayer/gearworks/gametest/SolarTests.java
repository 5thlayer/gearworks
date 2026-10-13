// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.GearFixtures.suitWith;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.wearer;

import io.github._5thlayer.gearworks.gear.Gear;
import io.github._5thlayer.gearworks.gear.GearContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Solar charges the worn suit in daylight under open sky, and not under a roof. Each test ticks its
 * player only once the level reads the sky it is about: sky light reaches the light engine some ticks
 * after a block does, and on a busy server, still lighting the chunks it just made, that has taken more
 * than five. Both run at noon, in one environment: a batch at midnight before them could wedge the
 * server's light engine for good, and nothing checks that solar stops at night until that is
 * understood (#8).
 */
final class SolarTests {

    private static final int WAIT = 20;
    private static final int MAX_TICKS = 100;
    private static final BlockPos ROOF = new BlockPos(1, 4, 1);

    private SolarTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        var clear = new TestEnvironmentDefinition.Weather(TestEnvironmentDefinition.Weather.Type.CLEAR);
        var day = tests.environment("noon", new DayTime(DayTime.NOON), clear);
        tests.test("solar_charges_in_daylight_under_open_sky", MAX_TICKS, day, SolarTests::chargesByDay);
        tests.test("solar_does_not_charge_under_a_roof", MAX_TICKS, day, SolarTests::shaded);
    }

    private static void chargesByDay(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SOLAR_UPGRADE.get());
        var player = wearer(helper, suit);
        tickUnder(helper, player, true, true)
                .thenWaitUntil(() -> helper.assertTrue(Gear.energy(suit) > 0, Component.literal("no FE from the sun")))
                .thenSucceed();
    }

    private static void shaded(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SOLAR_UPGRADE.get());
        var player = wearer(helper, suit);
        helper.setBlock(ROOF, Blocks.STONE);
        // Over the roof the sky is open: the light engine has reached this column, roof and all.
        var overRoof = helper.absolutePos(ROOF.above());
        noFeAfterAWhile(helper, suit, tickUnder(helper, player, true, false, () -> helper.assertTrue(
                helper.getLevel().canSeeSky(overRoof), Component.literal("the sky over the roof is not lit yet"))));
    }

    private static GameTestSequence tickUnder(GameTestHelper helper, Player player, boolean bright, boolean openSky) {
        return tickUnder(helper, player, bright, openSky, () -> {
        });
    }

    /**
     * Ticks the player each game tick from the one after the level reads that sky at its eyes, and
     * the sequence that waited for it, to go on from there.
     */
    private static GameTestSequence tickUnder(GameTestHelper helper, Player player, boolean bright, boolean openSky, Runnable alsoReady) {
        var ticking = new boolean[1];
        helper.onEachTick(() -> {
            if (ticking[0]) {
                player.tick();
            }
        });
        var level = helper.getLevel();
        return helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(level.isBrightOutside() == bright,
                            Component.literal(bright ? "not bright outside yet" : "still bright outside"));
                    helper.assertTrue(level.canSeeSky(BlockPos.containing(player.getEyePosition())) == openSky,
                            Component.literal(openSky ? "no sky at the player's eyes yet" : "still sky at the player's eyes"));
                    alsoReady.run();
                })
                .thenExecute(() -> ticking[0] = true);
    }

    private static void noFeAfterAWhile(GameTestHelper helper, ItemStack suit, GameTestSequence ticking) {
        ticking.thenIdle(WAIT)
                .thenExecute(() -> helper.assertValueEqual(Gear.energy(suit), 0, Component.literal("FE held")))
                .thenSucceed();
    }
}
