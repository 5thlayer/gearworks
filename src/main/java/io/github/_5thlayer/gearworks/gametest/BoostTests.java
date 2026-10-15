// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.SuitFixtures.addAtLevel;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.charge;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.tierWith;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.wearer;

import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.gear.Stat;
import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.gear.SuitBoost;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Boost throws a flying player along their gaze, as a firework does a glider, spending its draw and
 * starting a Cooldown that holds the next one back; it never fires on the ground or short of FE, and
 * its Levels make it cheaper, quicker to come back and stronger, each toward its limit.
 */
final class BoostTests {

    private static final double HEIGHT = 8;
    private static final int FULL = 100_000;

    private BoostTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("a_boost_spends_its_draw_and_throws_the_player_along_their_gaze", 40, BoostTests::throwsAlongGaze);
        tests.test("a_boost_in_its_cooldown_does_not_fire_and_the_next_does_once_it_ends", 200, BoostTests::coolsDown);
        tests.test("a_player_on_the_ground_cannot_boost", 1, BoostTests::notOnTheGround);
        tests.test("a_suit_short_of_a_boosts_fe_does_not_boost", 1, BoostTests::shortOfFe);
        tests.test("landing_ends_the_thrust_and_keeps_the_cooldown", 1, BoostTests::landingEndsThrust);
        tests.test("a_creative_player_boosts_for_free_and_still_waits_out_the_cooldown", 1, BoostTests::creativeIsFree);
        tests.test("a_raised_boost_costs_less_comes_back_sooner_and_pulls_harder", 1, BoostTests::raisedIsBetter);
    }

    private static void throwsAlongGaze(GameTestHelper helper) {
        var suit = booster(0);
        charge(suit, FULL);
        var player = aloft(helper, suit, GameType.SURVIVAL);
        double z = player.getZ();
        int draw = SuitFixtures.upgrade(GearContent.BOOST_UPGRADE.get()).draw();

        helper.assertTrue(SuitBoost.fire(player), Component.literal("the boost did not fire"));

        helper.assertValueEqual(Suit.energy(suit), FULL - draw, Component.literal("FE after the boost"));
        helper.onEachTick(player::tick);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(player.getAbilities().flying, Component.literal("the player stopped flying"));
            helper.assertTrue(player.getZ() - z > 4, Component.literal("the boost carried the player " + (player.getZ() - z) + " blocks in 10 ticks"));
            helper.assertTrue(Math.abs(player.getX() - helper.absoluteVec(new Vec3(1.5, 1, 1.5)).x) < 0.01,
                    Component.literal("the boost pushed the player sideways off their gaze"));
            helper.succeed();
        });
    }

    private static void coolsDown(GameTestHelper helper) {
        var suit = booster(0);
        charge(suit, FULL);
        var player = aloft(helper, suit, GameType.SURVIVAL);
        int cooldown = Suit.totals(suit).cooldown(Stat.BOOST);
        SuitBoost.fire(player);
        int afterOne = Suit.energy(suit);

        helper.assertFalse(SuitBoost.fire(player), Component.literal("a second boost fired in the cooldown"));

        helper.assertValueEqual(Suit.energy(suit), afterOne, Component.literal("FE after a boost in the cooldown"));
        int[] ticks = {0};
        helper.onEachTick(() -> {
            if (ticks[0] == cooldown) {
                return;
            }
            player.tick();
            ticks[0]++;
            if (ticks[0] < cooldown) {
                helper.assertFalse(SuitBoost.of(player).ready(), Component.literal("the cooldown ended after " + ticks[0] + " ticks"));
            } else {
                helper.assertTrue(SuitBoost.fire(player), Component.literal("no boost fired once the cooldown ended"));
                helper.succeed();
            }
        });
    }

    private static void notOnTheGround(GameTestHelper helper) {
        var suit = booster(0);
        charge(suit, FULL);
        var player = wearer(helper, suit);
        player.tick();

        helper.assertFalse(SuitBoost.fire(player), Component.literal("a boost fired on the ground"));

        helper.assertValueEqual(Suit.energy(suit), FULL, Component.literal("FE after a boost on the ground"));
        helper.succeed();
    }

    private static void shortOfFe(GameTestHelper helper) {
        var suit = booster(0);
        int draw = SuitFixtures.upgrade(GearContent.BOOST_UPGRADE.get()).draw();
        charge(suit, draw - 1);
        var player = aloft(helper, suit, GameType.SURVIVAL);

        helper.assertFalse(SuitBoost.fire(player), Component.literal("a boost fired short of FE"));

        helper.assertValueEqual(Suit.energy(suit), draw - 1, Component.literal("FE after a boost short of it"));
        helper.succeed();
    }

    private static void landingEndsThrust(GameTestHelper helper) {
        var suit = booster(0);
        charge(suit, FULL);
        var player = aloft(helper, suit, GameType.SURVIVAL);
        SuitBoost.fire(player);
        player.getAbilities().flying = false;

        player.tick();

        helper.assertFalse(SuitBoost.of(player).thrusting(), Component.literal("the thrust outlasted the flight"));
        helper.assertFalse(SuitBoost.of(player).ready(), Component.literal("landing ended the cooldown"));
        helper.succeed();
    }

    private static void creativeIsFree(GameTestHelper helper) {
        var suit = booster(0);
        charge(suit, FULL);
        var player = aloft(helper, suit, GameType.CREATIVE);

        helper.assertTrue(SuitBoost.fire(player), Component.literal("a creative player's boost did not fire"));
        helper.assertValueEqual(Suit.energy(suit), FULL, Component.literal("FE after a creative boost"));
        helper.assertFalse(SuitBoost.fire(player), Component.literal("a creative player's boost skipped the cooldown"));
        helper.succeed();
    }

    private static void raisedIsBetter(GameTestHelper helper) {
        var fresh = Suit.totals(booster(0));
        var raised = Suit.totals(booster(10));
        int floor = GearworksConfig.floor(Stat.BOOST);
        int cooldownFloor = GearworksConfig.boostCooldownFloor();
        double ceiling = GearworksConfig.ceiling(Stat.BOOST);

        helper.assertTrue(raised.draw(Stat.BOOST) < fresh.draw(Stat.BOOST),
                Component.literal("Level 10 spends " + raised.draw(Stat.BOOST) + " FE, Level 0 " + fresh.draw(Stat.BOOST)));
        helper.assertTrue(raised.draw(Stat.BOOST) >= floor, Component.literal("Level 10 spends under the Floor " + floor));
        helper.assertTrue(raised.cooldown(Stat.BOOST) < fresh.cooldown(Stat.BOOST),
                Component.literal("Level 10 waits " + raised.cooldown(Stat.BOOST) + " ticks, Level 0 " + fresh.cooldown(Stat.BOOST)));
        helper.assertTrue(raised.cooldown(Stat.BOOST) >= cooldownFloor, Component.literal("Level 10 waits under " + cooldownFloor + " ticks"));
        helper.assertTrue(raised.underCeiling(Stat.BOOST) > fresh.underCeiling(Stat.BOOST), Component.literal("Level 10 pulls no harder"));
        helper.assertTrue(raised.underCeiling(Stat.BOOST) < ceiling, Component.literal("Level 10 reaches the Ceiling " + ceiling));
        helper.succeed();
    }

    /** A netherite suit holding a battery, a flight upgrade, and a boost upgrade at that Level. */
    private static ItemStack booster(int level) {
        var suit = tierWith(GearContent.NETHERITE_SUIT.get(), GearContent.BATTERY_UPGRADE.get(), GearContent.FLIGHT_UPGRADE.get());
        addAtLevel(suit, GearContent.BOOST_UPGRADE.get(), level);
        return suit;
    }

    /** A player in that game mode wearing the suit, flying {@link #HEIGHT} blocks over the platform and facing south. */
    private static Player aloft(GameTestHelper helper, ItemStack suit, GameType mode) {
        var player = wearer(helper, suit, mode);
        var air = player.position().add(0, HEIGHT, 0);
        player.setPos(air);
        player.tick();
        helper.assertTrue(player.mayFly(), Component.literal("the suit gives no flight"));
        player.setPos(air);
        player.setDeltaMovement(Vec3.ZERO);
        player.setYRot(0);
        player.setXRot(0);
        player.getAbilities().flying = true;
        return player;
    }
}
