// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.SuitFixtures.addAtLevel;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.charge;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.tierWith;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.wearer;

import java.util.List;

import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.gear.Stat;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Flight, the capstone: it fits only a tier-3 grid, flies while the suit has the FE it draws each tick
 * in the air, and a suit that runs dry mid-air lets the player down slowly, at no cost and unharmed.
 * Its Levels lower the draw and raise how fast the player flies, from about half of creative's speed
 * (ADR 0006), which the suit sets only while it alone lets the player fly.
 */
final class FlightTests {

    // High enough that a plain fall from it hurts.
    private static final double HEIGHT = 8;

    private FlightTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("flight_fits_a_tier_3_grid_and_no_smaller_one", 1, FlightTests::fitsTierThreeOnly);
        tests.test("a_flying_player_stays_aloft_while_the_suit_draws_fe", 40, FlightTests::staysAloft);
        tests.test("a_suit_that_runs_dry_mid_air_lets_the_player_down_slowly_unharmed", 200, FlightTests::runsDry);
        tests.test("a_raised_flight_upgrade_draws_less_and_flies_faster", 1, FlightTests::raisedDrawsLess);
        tests.test("taking_the_suit_off_puts_creative_flying_speed_back", 1, FlightTests::speedComesBack);
        tests.test("a_creative_player_in_the_suit_flies_at_creative_speed", 1, FlightTests::creativeKeepsSpeed);
        tests.test("a_suit_without_fe_gives_no_flight", 1, FlightTests::noFlightWhenEmpty);
    }

    private static void fitsTierThreeOnly(GameTestHelper helper) {
        var flight = SuitFixtures.upgrade(GearContent.FLIGHT_UPGRADE.get());
        var item = new ItemStackTemplate(GearContent.FLIGHT_UPGRADE.get());
        for (var tier : List.of(GearContent.IRON_SUIT.get(), GearContent.DIAMOND_SUIT.get())) {
            helper.assertTrue(Suit.grid(new ItemStack(tier)).placeFirstFit(flight.width(), flight.height(), item).isEmpty(),
                    Component.literal("flight fits " + tier));
        }
        helper.assertTrue(Suit.grid(new ItemStack(GearContent.NETHERITE_SUIT.get())).placeFirstFit(flight.width(), flight.height(), item).isPresent(),
                Component.literal("flight does not fit the netherite suit"));
        helper.succeed();
    }

    private static void staysAloft(GameTestHelper helper) {
        var suit = flyer(0);
        charge(suit, 100_000);
        var player = aloft(helper, suit);
        double height = player.getY();
        int draw = SuitFixtures.upgrade(GearContent.FLIGHT_UPGRADE.get()).draw();
        helper.onEachTick(player::tick);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(player.getAbilities().flying, Component.literal("the player stopped flying"));
            helper.assertTrue(Math.abs(player.getY() - height) < 0.01, Component.literal("the player sank from " + height + " to " + player.getY()));
            helper.assertTrue(Suit.energy(suit) <= 100_000 - 20 * draw, Component.literal("flight drew only " + (100_000 - Suit.energy(suit)) + " FE in 20 ticks"));
            helper.succeed();
        });
    }

    private static void runsDry(GameTestHelper helper) {
        var suit = flyer(0);
        charge(suit, 3 * SuitFixtures.upgrade(GearContent.FLIGHT_UPGRADE.get()).draw());
        var player = aloft(helper, suit);
        float health = player.getHealth();
        helper.onEachTick(() -> {
            player.tick();
            if (!player.onGround()) {
                helper.assertTrue(player.getDeltaMovement().y > -0.6,
                        Component.literal("the player fell at " + player.getDeltaMovement().y + " blocks a tick"));
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(player.onGround(), Component.literal("the player is still in the air"));
            helper.assertValueEqual(Suit.energy(suit), 0, Component.literal("FE left"));
            helper.assertFalse(player.getAbilities().flying, Component.literal("the player still flies in an empty suit"));
            helper.assertFalse(player.hasEffect(MobEffects.SLOW_FALLING), Component.literal("slow falling outlasted the landing"));
            helper.assertValueEqual(player.getHealth(), health, Component.literal("health after landing"));
        });
    }

    private static void raisedDrawsLess(GameTestHelper helper) {
        var fresh = flyer(0);
        var raised = flyer(10);
        float speed = new Abilities().getFlyingSpeed();
        int floor = GearworksConfig.floor(Stat.FLIGHT);
        int[] draws = new int[2];
        float[] speeds = new float[2];
        var suits = List.of(fresh, raised);
        for (int i = 0; i < 2; i++) {
            charge(suits.get(i), 100_000);
            var player = aloft(helper, suits.get(i));
            player.tick();
            draws[i] = 100_000 - Suit.energy(suits.get(i));
            speeds[i] = player.getAbilities().getFlyingSpeed();
        }

        helper.assertTrue(draws[1] < draws[0], Component.literal("Level 10 drew " + draws[1] + " FE, Level 0 " + draws[0]));
        helper.assertTrue(draws[1] >= floor, Component.literal("Level 10 drew " + draws[1] + " FE, under the Floor " + floor));
        helper.assertTrue(speeds[0] < speed, Component.literal("Level 0 flies at " + speeds[0] + ", creative's speed " + speed));
        helper.assertTrue(speeds[1] > speeds[0], Component.literal("Level 10 flies at " + speeds[1] + ", Level 0 at " + speeds[0]));
        float ceiling = speed * (float) GearworksConfig.ceiling(Stat.FLIGHT);
        helper.assertTrue(speeds[1] < ceiling, Component.literal("Level 10 flies at " + speeds[1] + ", over the Ceiling " + ceiling));
        helper.succeed();
    }

    private static void speedComesBack(GameTestHelper helper) {
        var suit = flyer(0);
        charge(suit, 100_000);
        var player = aloft(helper, suit);
        helper.assertTrue(player.getAbilities().getFlyingSpeed() != new Abilities().getFlyingSpeed(),
                Component.literal("the suit left creative's flying speed"));

        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.tick();

        helper.assertValueEqual(player.getAbilities().getFlyingSpeed(), new Abilities().getFlyingSpeed(), Component.literal("flying speed without the suit"));
        helper.succeed();
    }

    private static void creativeKeepsSpeed(GameTestHelper helper) {
        var suit = flyer(0);
        charge(suit, 100_000);
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
        player.setItemSlot(EquipmentSlot.CHEST, suit);

        player.tick();

        helper.assertValueEqual(player.getAbilities().getFlyingSpeed(), new Abilities().getFlyingSpeed(), Component.literal("a creative player's flying speed"));
        helper.succeed();
    }

    private static void noFlightWhenEmpty(GameTestHelper helper) {
        var player = wearer(helper, flyer(0));

        player.tick();

        helper.assertFalse(player.mayFly(), Component.literal("an empty suit gives flight"));
        helper.succeed();
    }

    /** A netherite suit holding a battery and a flight upgrade at that Level. */
    private static ItemStack flyer(int level) {
        var suit = tierWith(GearContent.NETHERITE_SUIT.get(), GearContent.BATTERY_UPGRADE.get());
        addAtLevel(suit, GearContent.FLIGHT_UPGRADE.get(), level);
        return suit;
    }

    /** A player wearing the suit, flying {@link #HEIGHT} blocks over the platform once the suit lets them. */
    private static Player aloft(GameTestHelper helper, ItemStack suit) {
        var player = wearer(helper, suit);
        var air = player.position().add(0, HEIGHT, 0);
        player.setPos(air);
        player.tick();
        helper.assertTrue(player.mayFly(), Component.literal("the suit gives no flight"));
        // Back where it was, before the tick's fall.
        player.setPos(air);
        player.setDeltaMovement(Vec3.ZERO);
        player.getAbilities().flying = true;
        return player;
    }
}
