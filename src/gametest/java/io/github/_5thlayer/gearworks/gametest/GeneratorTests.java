// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.SuitFixtures.addAtLevel;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.charge;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.suitWith;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.upgrade;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.wearer;

import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.gear.Suit;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Generators other than solar, in a worn suit: nuclear charges every tick; kinetic as the player
 * moves on foot, jumping and wading included, and never under water or riding; combustion while it
 * burns tagged fuel from the main inventory before the hotbar, keeping its fire on the suit; and
 * metabolic from saturation, never hunger. Upgrades of one Generator add up, and a fraction of an FE
 * waits for the next tick. A full suit makes and spends nothing. {@code SolarTests} has solar; no test
 * glides, since gliding needs an elytra in the slot the suit fills.
 */
final class GeneratorTests {

    private GeneratorTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("generator_nuclear_charges_every_tick", 1, GeneratorTests::nuclear);
        tests.test("generator_two_nuclear_upgrades_add_up", 1, GeneratorTests::twoNuclear);
        tests.test("generator_kinetic_charges_while_walking", 60, GeneratorTests::kineticWalking);
        tests.test("generator_kinetic_charges_nothing_standing_still", 20, GeneratorTests::kineticStill);
        tests.test("generator_kinetic_charges_nothing_while_flying", 60, GeneratorTests::kineticFlying);
        tests.test("generator_kinetic_charges_in_the_air_of_a_jump", 1, GeneratorTests::kineticJumping);
        tests.test("generator_kinetic_charges_wading_through_shallow_water", 60, GeneratorTests::kineticWading);
        tests.test("generator_kinetic_charges_nothing_under_water", 30, GeneratorTests::kineticUnderWater);
        tests.test("generator_kinetic_charges_nothing_while_riding", 30, GeneratorTests::kineticRiding);
        tests.test("generator_combustion_burns_tagged_fuel_from_the_main_inventory_first", 1, GeneratorTests::combustionFirst);
        tests.test("generator_two_combustion_upgrades_burn_twice_as_fast_for_twice_the_fe", 1, GeneratorTests::twoCombustion);
        tests.test("generator_a_levelled_combustion_upgrade_keeps_its_half_fe", 1, GeneratorTests::combustionFraction);
        tests.test("generator_combustion_leaves_untagged_fuel_alone", 20, GeneratorTests::combustionUntagged);
        tests.test("generator_combustion_keeps_its_fire_on_the_suit", 40, GeneratorTests::combustionKeepsFire);
        tests.test("generator_metabolic_spends_saturation_and_never_hunger", 20, GeneratorTests::metabolic);
        tests.test("generator_metabolic_stops_when_saturation_runs_out", 40, GeneratorTests::metabolicRunsOut);
        tests.test("generator_two_metabolic_upgrades_add_up", 1, GeneratorTests::twoMetabolic);
        tests.test("generator_a_full_suit_makes_and_spends_nothing", 20, GeneratorTests::full);
    }

    private static void nuclear(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.NUCLEAR_UPGRADE.get());
        var player = wearer(helper, suit);
        player.tick();
        helper.assertValueEqual(Suit.energy(suit), (int) upgrade(GearContent.NUCLEAR_UPGRADE.get()).amount(), Component.literal("FE after a tick"));
        helper.succeed();
    }

    private static void twoNuclear(GameTestHelper helper) {
        var suit = suitWith(GearContent.NUCLEAR_UPGRADE.get(), GearContent.NUCLEAR_UPGRADE.get(), GearContent.BATTERY_UPGRADE.get());
        var player = wearer(helper, suit);
        player.tick();
        helper.assertValueEqual(Suit.energy(suit), 2 * (int) upgrade(GearContent.NUCLEAR_UPGRADE.get()).amount(),
                Component.literal("FE after a tick"));
        helper.succeed();
    }

    private static void kineticWalking(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.KINETIC_UPGRADE.get());
        var player = wearer(helper, suit);
        helper.onEachTick(() -> {
            // Walking keeps gravity's pull, which holds the player on the ground.
            player.setDeltaMovement(new Vec3(0.2, player.getDeltaMovement().y, 0));
            player.tick();
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(player.onGround(), Component.literal("the player is not on the ground"));
            helper.assertTrue(Suit.energy(suit) > 0, Component.literal("no FE from walking"));
        });
    }

    private static void kineticStill(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.KINETIC_UPGRADE.get());
        var player = wearer(helper, suit);
        helper.onEachTick(player::tick);
        helper.runAfterDelay(15, () -> {
            helper.assertValueEqual(Suit.energy(suit), 0, Component.literal("FE standing still"));
            helper.succeed();
        });
    }

    private static void kineticFlying(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.KINETIC_UPGRADE.get());
        // Creative lets the player fly with no flight upgrade to draw FE.
        var player = wearer(helper, suit, GameType.CREATIVE);
        player.getAbilities().flying = true;
        helper.onEachTick(() -> {
            player.setDeltaMovement(new Vec3(0.2, 0, 0));
            player.tick();
        });
        helper.runAfterDelay(30, () -> {
            helper.assertValueEqual(Suit.energy(suit), 0, Component.literal("FE while flying"));
            helper.succeed();
        });
    }

    private static void kineticJumping(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.KINETIC_UPGRADE.get());
        var player = wearer(helper, suit);
        var air = helper.absoluteVec(new Vec3(1.5, 2.5, 1.5));
        player.setPos(air.x, air.y, air.z);
        player.setDeltaMovement(new Vec3(0.2, 0, 0));
        player.tick();
        helper.assertFalse(player.onGround(), Component.literal("the player landed"));
        helper.assertTrue(Suit.energy(suit) > 0, Component.literal("no FE in the air"));
        helper.succeed();
    }

    private static void kineticWading(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.WATER);
        helper.setBlock(2, 1, 1, Blocks.WATER);
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.KINETIC_UPGRADE.get());
        var player = wearer(helper, suit);
        helper.onEachTick(() -> {
            player.setDeltaMovement(new Vec3(0.05, player.getDeltaMovement().y, 0));
            player.tick();
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(player.isInWater(), Component.literal("the player is not in the water"));
            helper.assertTrue(Suit.energy(suit) > 0, Component.literal("no FE wading"));
        });
    }

    private static void kineticUnderWater(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.WATER);
        helper.setBlock(1, 2, 1, Blocks.WATER);
        helper.setBlock(1, 3, 1, Blocks.WATER);
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.KINETIC_UPGRADE.get());
        var player = wearer(helper, suit);
        int[] under = new int[1];
        helper.onEachTick(() -> {
            // Moving no further than the water's own block.
            player.setDeltaMovement(new Vec3(0.002, player.getDeltaMovement().y, 0.002));
            player.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
            player.tick();
            if (player.isUnderWater()) {
                under[0]++;
            }
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(under[0] > 0, Component.literal("the player was never under water"));
            helper.assertValueEqual(Suit.energy(suit), 0, Component.literal("FE under water"));
            helper.succeed();
        });
    }

    private static void kineticRiding(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.KINETIC_UPGRADE.get());
        var player = wearer(helper, suit);
        var cart = helper.spawn(EntityType.MINECART, new BlockPos(1, 1, 1));
        helper.assertTrue(player.startRiding(cart, true, false), Component.literal("the player did not get in the minecart"));
        helper.onEachTick(() -> {
            player.setDeltaMovement(new Vec3(0.2, 0, 0));
            cart.setDeltaMovement(new Vec3(0.2, 0, 0));
            player.tick();
        });
        helper.runAfterDelay(20, () -> {
            helper.assertValueEqual(Suit.energy(suit), 0, Component.literal("FE while riding"));
            helper.succeed();
        });
    }

    private static void twoCombustion(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.COMBUSTION_UPGRADE.get(), GearContent.COMBUSTION_UPGRADE.get());
        var player = wearer(helper, suit);
        player.getInventory().setItem(9, new ItemStack(Items.COAL));
        player.tick();
        helper.assertValueEqual(Suit.energy(suit), 2 * (int) upgrade(GearContent.COMBUSTION_UPGRADE.get()).amount(), Component.literal("FE after a tick"));
        helper.assertValueEqual(suit.getOrDefault(GearContent.COMBUSTION_FIRE.get(), 0), 1598, Component.literal("the fire's burn time left"));
        helper.succeed();
    }

    private static void combustionFraction(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get());
        addAtLevel(suit, GearContent.COMBUSTION_UPGRADE.get(), 1);
        var player = wearer(helper, suit);
        player.getInventory().setItem(9, new ItemStack(Items.COAL));
        // 2.5 FE a tick at Level 1.
        player.tick();
        helper.assertValueEqual(Suit.energy(suit), 2, Component.literal("FE after a tick"));
        player.tick();
        helper.assertValueEqual(Suit.energy(suit), 5, Component.literal("FE after two ticks"));
        helper.succeed();
    }

    private static void combustionFirst(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.COMBUSTION_UPGRADE.get());
        var player = wearer(helper, suit);
        var inventory = player.getInventory();
        inventory.setItem(0, new ItemStack(Items.COAL));
        inventory.setItem(9, new ItemStack(Items.OAK_PLANKS));
        inventory.setItem(10, new ItemStack(Items.COAL));
        player.tick();
        helper.assertTrue(inventory.getItem(10).isEmpty(), Component.literal("the main inventory's coal is not burning"));
        helper.assertValueEqual(inventory.getItem(0).getCount(), 1, Component.literal("coal left in the hotbar"));
        helper.assertValueEqual(inventory.getItem(9).getCount(), 1, Component.literal("planks left"));
        helper.assertValueEqual(Suit.energy(suit), (int) upgrade(GearContent.COMBUSTION_UPGRADE.get()).amount(), Component.literal("FE after a tick"));
        helper.assertValueEqual(suit.getOrDefault(GearContent.COMBUSTION_FIRE.get(), 0), 1599, Component.literal("the fire's burn time left"));
        helper.succeed();
    }

    private static void combustionUntagged(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.COMBUSTION_UPGRADE.get());
        var player = wearer(helper, suit);
        player.getInventory().setItem(9, new ItemStack(Items.OAK_PLANKS, 4));
        helper.onEachTick(player::tick);
        helper.runAfterDelay(10, () -> {
            helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 4, Component.literal("planks left"));
            helper.assertValueEqual(Suit.energy(suit), 0, Component.literal("FE from planks"));
            helper.succeed();
        });
    }

    private static void combustionKeepsFire(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.COMBUSTION_UPGRADE.get());
        var player = wearer(helper, suit);
        var inventory = player.getInventory();
        inventory.setItem(9, new ItemStack(Items.COAL, 2));
        player.tick();
        int fire = suit.getOrDefault(GearContent.COMBUSTION_FIRE.get(), 0);
        int energy = Suit.energy(suit);
        helper.assertTrue(fire > 0, Component.literal("no fire"));
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        helper.onEachTick(player::tick);
        helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    helper.assertValueEqual(suit.getOrDefault(GearContent.COMBUSTION_FIRE.get(), 0), fire, Component.literal("the fire off the player"));
                    helper.assertValueEqual(Suit.energy(suit), energy, Component.literal("FE off the player"));
                    player.setItemSlot(EquipmentSlot.CHEST, suit);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    helper.assertTrue(Suit.energy(suit) > energy, Component.literal("no FE once worn again"));
                    helper.assertValueEqual(inventory.getItem(9).getCount(), 1, Component.literal("coal left"));
                })
                .thenSucceed();
    }

    private static void metabolic(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.METABOLIC_UPGRADE.get());
        var player = wearer(helper, suit);
        var food = player.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(5);
        helper.onEachTick(player::tick);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(Suit.energy(suit) > 0, Component.literal("no FE from saturation"));
            helper.assertTrue(food.getSaturationLevel() < 5, Component.literal("no saturation spent"));
            helper.assertValueEqual(food.getFoodLevel(), 20, Component.literal("hunger"));
            helper.succeed();
        });
    }

    private static void metabolicRunsOut(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.METABOLIC_UPGRADE.get());
        var player = wearer(helper, suit);
        var food = player.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(0.1f);
        helper.onEachTick(player::tick);
        helper.runAfterDelay(30, () -> {
            // What saturation is left is worth less than one FE.
            helper.assertTrue(food.getSaturationLevel() < 1.0f / GearworksConfig.metabolicEnergy(), Component.literal("saturation left"));
            helper.assertValueEqual(food.getFoodLevel(), 20, Component.literal("hunger"));
            int energy = Suit.energy(suit);
            helper.assertTrue(energy > 90 && energy <= 100, Component.literal("FE from 0.1 saturation: " + energy));
            helper.succeed();
        });
    }

    private static void twoMetabolic(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.METABOLIC_UPGRADE.get(), GearContent.METABOLIC_UPGRADE.get());
        var player = wearer(helper, suit);
        player.getFoodData().setSaturation(5);
        player.tick();
        helper.assertValueEqual(Suit.energy(suit), 2 * (int) upgrade(GearContent.METABOLIC_UPGRADE.get()).amount(), Component.literal("FE after a tick"));
        helper.succeed();
    }

    private static void full(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.COMBUSTION_UPGRADE.get(), GearContent.METABOLIC_UPGRADE.get(),
                GearContent.NUCLEAR_UPGRADE.get());
        int capacity = Suit.capacity(suit);
        charge(suit, capacity);
        var player = wearer(helper, suit);
        player.getInventory().setItem(9, new ItemStack(Items.COAL));
        player.getFoodData().setSaturation(5);
        helper.onEachTick(player::tick);
        helper.runAfterDelay(10, () -> {
            helper.assertValueEqual(Suit.energy(suit), capacity, Component.literal("FE"));
            helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, Component.literal("coal left"));
            helper.assertValueEqual(player.getFoodData().getSaturationLevel(), 5.0f, Component.literal("saturation"));
            helper.assertValueEqual(suit.getOrDefault(GearContent.COMBUSTION_FIRE.get(), 0), 0, Component.literal("the fire"));
            helper.succeed();
        });
    }
}
