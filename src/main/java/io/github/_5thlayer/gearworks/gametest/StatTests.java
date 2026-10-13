// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.GearFixtures.addAtLevel;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.charge;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.suitWith;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.tierWith;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.wearer;

import java.util.List;

import io.github._5thlayer.gearworks.gear.Gear;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.gear.Stat;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The shield, jump and night vision Stats, each of which runs only while the suit has the FE to pay
 * for it; several shield or jump upgrades add to one another and stay under their Stat's Ceiling.
 */
final class StatTests {

    private static final float HIT = 10;

    private StatTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("the_shield_absorbs_damage_and_spends_fe_for_it", 1, StatTests::shieldWithEnergy);
        tests.test("the_shield_absorbs_nothing_in_an_empty_suit", 1, StatTests::noShieldWhenEmpty);
        tests.test("jump_rises_and_each_jump_spends_fe_while_the_suit_has_it", 1, StatTests::jumpWithEnergy);
        tests.test("jump_stays_at_base_in_an_empty_suit", 1, StatTests::noJumpWhenEmpty);
        tests.test("several_shields_absorb_more_than_one_and_stay_under_the_ceiling", 1, StatTests::shieldCeiling);
        tests.test("several_jump_upgrades_rise_more_than_one_and_stay_under_the_ceiling", 1, StatTests::jumpCeiling);
        tests.test("night_vision_runs_while_the_suit_has_fe_and_draws_it", 1, StatTests::nightVisionWithEnergy);
        tests.test("night_vision_stops_when_the_suit_runs_empty", 20, StatTests::nightVisionRunsOut);
        tests.test("an_empty_suit_leaves_a_potions_night_vision_alone", 1, StatTests::potionNightVisionKept);
    }

    private static void shieldWithEnergy(GameTestHelper helper) {
        var shielded = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SHIELD_UPGRADE.get());
        charge(shielded, 10_000);
        var bare = suitWith(GearContent.BATTERY_UPGRADE.get());
        charge(bare, 10_000);

        float taken = healthLost(helper, shielded);
        float takenBare = healthLost(helper, bare);

        helper.assertTrue(taken < takenBare, Component.literal("the shield took nothing: " + taken + " lost against " + takenBare));
        helper.assertTrue(Gear.energy(shielded) < 10_000, Component.literal("the shield spent no FE"));
        helper.assertValueEqual(Gear.energy(bare), 10_000, Component.literal("FE in a suit with no shield"));
        helper.succeed();
    }

    private static void noShieldWhenEmpty(GameTestHelper helper) {
        var shielded = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SHIELD_UPGRADE.get());
        var bare = suitWith(GearContent.BATTERY_UPGRADE.get());

        helper.assertValueEqual(healthLost(helper, shielded), healthLost(helper, bare), Component.literal("health lost"));
        helper.assertValueEqual(Gear.energy(shielded), 0, Component.literal("FE in the empty suit"));
        helper.succeed();
    }

    private static void jumpWithEnergy(GameTestHelper helper) {
        var jumper = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.JUMP_UPGRADE.get());
        charge(jumper, 10_000);
        var bare = suitWith(GearContent.BATTERY_UPGRADE.get());
        charge(bare, 10_000);
        int draw = GearFixtures.upgrade(GearContent.JUMP_UPGRADE.get()).draw();
        helper.assertTrue(draw > 0, Component.literal("the jump upgrade draws no FE"));

        double rise = jumpSpeed(helper, jumper);
        double riseBare = jumpSpeed(helper, bare);

        helper.assertTrue(rise > riseBare, Component.literal("the jump did not rise: " + rise + " against " + riseBare));
        helper.assertValueEqual(Gear.energy(jumper), 10_000 - draw, Component.literal("FE after one jump"));
        helper.succeed();
    }

    private static void noJumpWhenEmpty(GameTestHelper helper) {
        var jumper = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.JUMP_UPGRADE.get());
        var bare = suitWith(GearContent.BATTERY_UPGRADE.get());

        helper.assertValueEqual(jumpSpeed(helper, jumper), jumpSpeed(helper, bare), Component.literal("the jump's upward speed"));
        helper.assertValueEqual(Gear.energy(jumper), 0, Component.literal("FE in the empty suit"));
        helper.succeed();
    }

    private static void shieldCeiling(GameTestHelper helper) {
        var one = tierWith(GearContent.NETHERITE_GEAR.get(), GearContent.BATTERY_UPGRADE.get());
        addAtLevel(one, GearContent.SHIELD_UPGRADE.get(), 10);
        var several = tierWith(GearContent.NETHERITE_GEAR.get(), GearContent.BATTERY_UPGRADE.get());
        for (int i = 0; i < 3; i++) {
            addAtLevel(several, GearContent.SHIELD_UPGRADE.get(), 10);
        }
        var bare = tierWith(GearContent.NETHERITE_GEAR.get(), GearContent.BATTERY_UPGRADE.get());
        for (var suit : List.of(one, several, bare)) {
            charge(suit, 100_000);
        }

        float takenOne = healthLost(helper, one);
        float takenSeveral = healthLost(helper, several);
        float takenBare = healthLost(helper, bare);

        helper.assertTrue(takenSeveral < takenOne, Component.literal("three shields absorbed no more than one"));
        double absorbed = 1 - takenSeveral / takenBare;
        helper.assertTrue(absorbed < GearworksConfig.ceiling(Stat.SHIELD),
                Component.literal("three shields absorbed " + absorbed + ", over the Ceiling " + GearworksConfig.ceiling(Stat.SHIELD)));
        helper.succeed();
    }

    private static void jumpCeiling(GameTestHelper helper) {
        var one = tierWith(GearContent.NETHERITE_GEAR.get(), GearContent.BATTERY_UPGRADE.get());
        addAtLevel(one, GearContent.JUMP_UPGRADE.get(), 10);
        var several = tierWith(GearContent.NETHERITE_GEAR.get(), GearContent.BATTERY_UPGRADE.get());
        for (int i = 0; i < 4; i++) {
            addAtLevel(several, GearContent.JUMP_UPGRADE.get(), 10);
        }
        var bare = tierWith(GearContent.NETHERITE_GEAR.get(), GearContent.BATTERY_UPGRADE.get());
        for (var suit : List.of(one, several, bare)) {
            charge(suit, 100_000);
        }

        double riseOne = jumpSpeed(helper, one);
        double riseSeveral = jumpSpeed(helper, several);
        double riseBare = jumpSpeed(helper, bare);

        helper.assertTrue(riseSeveral > riseOne, Component.literal("four jump upgrades rose no higher than one"));
        double added = riseSeveral / riseBare - 1;
        helper.assertTrue(added < GearworksConfig.ceiling(Stat.JUMP),
                Component.literal("four jump upgrades added " + added + ", over the Ceiling " + GearworksConfig.ceiling(Stat.JUMP)));
        helper.succeed();
    }

    private static void nightVisionWithEnergy(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.NIGHT_VISION_UPGRADE.get());
        charge(suit, 1000);
        int draw = GearFixtures.upgrade(GearContent.NIGHT_VISION_UPGRADE.get()).draw();
        helper.assertTrue(draw > 0, Component.literal("the night vision upgrade draws no FE"));
        var player = wearer(helper, suit);

        player.tick();

        helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), Component.literal("no night vision while the suit has FE"));
        helper.assertValueEqual(Gear.energy(suit), 1000 - draw, Component.literal("FE after a tick"));
        helper.succeed();
    }

    private static void nightVisionRunsOut(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.NIGHT_VISION_UPGRADE.get());
        charge(suit, 3 * GearFixtures.upgrade(GearContent.NIGHT_VISION_UPGRADE.get()).draw());
        var player = wearer(helper, suit);
        player.tick();
        helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), Component.literal("no night vision while the suit has FE"));
        helper.onEachTick(player::tick);
        helper.succeedWhen(() -> {
            helper.assertValueEqual(Gear.energy(suit), 0, Component.literal("FE left"));
            helper.assertFalse(player.hasEffect(MobEffects.NIGHT_VISION), Component.literal("night vision in an empty suit"));
        });
    }

    private static void potionNightVisionKept(GameTestHelper helper) {
        var player = wearer(helper, suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.NIGHT_VISION_UPGRADE.get()));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600));

        player.tick();

        helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), Component.literal("the empty suit took the potion's night vision"));
        helper.succeed();
    }

    /** The upward speed a player wearing the suit leaves the ground with, which sets how high they jump. */
    private static double jumpSpeed(GameTestHelper helper, ItemStack suit) {
        Player player = wearer(helper, suit);
        player.tick();
        player.setDeltaMovement(Vec3.ZERO);
        player.jumpFromGround();
        return player.getDeltaMovement().y;
    }

    /** The health a player wearing the suit loses to one hit. */
    private static float healthLost(GameTestHelper helper, ItemStack suit) {
        Player player = wearer(helper, suit);
        float before = player.getHealth();
        player.hurtServer(helper.getLevel(), player.damageSources().generic(), HIT);
        return before - player.getHealth();
    }
}
