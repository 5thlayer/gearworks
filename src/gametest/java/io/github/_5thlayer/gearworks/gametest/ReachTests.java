// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.SuitFixtures.addAtLevel;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.charge;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.suitWith;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.tierWith;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.wearer;

import java.util.List;

import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.gear.LevelCost;
import io.github._5thlayer.gearworks.gear.Stat;
import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.level.Curves;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.Tags;

/**
 * Reach lengthens the wearer's block and entity reach by the same blocks while the suit pays its draw
 * each tick, and several reach upgrades stay under the Ceiling together.
 */
final class ReachTests {

    private static final double TOLERANCE = 1.0E-9;

    private ReachTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("reach_lengthens_both_reaches_while_the_suit_has_fe", 1, ReachTests::reachWithEnergy);
        tests.test("reach_spends_its_draw_each_tick", 1, ReachTests::spendsEachTick);
        tests.test("reach_stays_at_base_in_an_empty_suit", 1, ReachTests::noReachWhenEmpty);
        tests.test("reach_goes_back_to_base_when_the_suit_comes_off", 1, ReachTests::noReachWithoutSuit);
        tests.test("reach_goes_back_to_base_when_the_suit_runs_empty", 20, ReachTests::reachRunsOut);
        tests.test("reach_of_several_upgrades_stays_under_the_ceiling", 1, ReachTests::reachCeiling);
        tests.test("reach_rises_and_draws_more_with_its_level", 1, ReachTests::raisedReach);
        tests.test("reach_upgrade_is_crafted_from_iron_ender_pearls_a_piston_and_redstone", 1, ReachTests::crafts);
        tests.test("reach_upgrade_levels_cost_ender_pearls_early", 1, ReachTests::levelCost);
    }

    private static void reachWithEnergy(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.REACH_UPGRADE.get());
        charge(suit, 1000);
        var player = wearer(helper, suit);

        player.tick();

        double added = Curves.underCeiling(SuitFixtures.upgrade(GearContent.REACH_UPGRADE.get()).amount(), GearworksConfig.ceiling(Stat.REACH));
        helper.assertTrue(added > 0, Component.literal("a reach upgrade adds no reach"));
        assertReach(helper, player, added, "with a charged reach upgrade");
        helper.succeed();
    }

    private static void spendsEachTick(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.REACH_UPGRADE.get());
        charge(suit, 1000);
        int draw = SuitFixtures.upgrade(GearContent.REACH_UPGRADE.get()).draw();
        helper.assertValueEqual(draw, 10, Component.literal("the reach upgrade's draw at Level 0"));
        var player = wearer(helper, suit);

        player.tick();
        player.tick();

        helper.assertValueEqual(Suit.energy(suit), 1000 - 2 * draw, Component.literal("FE after two ticks standing still"));
        helper.succeed();
    }

    private static void noReachWhenEmpty(GameTestHelper helper) {
        var player = wearer(helper, suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.REACH_UPGRADE.get()));

        player.tick();

        assertReach(helper, player, 0, "in an empty suit");
        helper.succeed();
    }

    private static void noReachWithoutSuit(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.REACH_UPGRADE.get());
        charge(suit, 1000);
        var player = wearer(helper, suit);
        player.tick();

        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.tick();

        assertReach(helper, player, 0, "with the suit taken off");
        helper.succeed();
    }

    private static void reachRunsOut(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.REACH_UPGRADE.get());
        charge(suit, 3 * SuitFixtures.upgrade(GearContent.REACH_UPGRADE.get()).draw());
        var player = wearer(helper, suit);
        player.tick();
        helper.assertTrue(blockReach(player) > base(player, true), Component.literal("no reach while the suit has FE"));
        helper.onEachTick(player::tick);
        helper.succeedWhen(() -> {
            helper.assertValueEqual(Suit.energy(suit), 0, Component.literal("FE left"));
            assertReach(helper, player, 0, "in a suit run empty");
        });
    }

    private static void reachCeiling(GameTestHelper helper) {
        var one = tierWith(GearContent.NETHERITE_SUIT.get(), GearContent.BATTERY_UPGRADE.get());
        addAtLevel(one, GearContent.REACH_UPGRADE.get(), 10);
        var several = tierWith(GearContent.NETHERITE_SUIT.get(), GearContent.BATTERY_UPGRADE.get());
        for (int i = 0; i < 4; i++) {
            addAtLevel(several, GearContent.REACH_UPGRADE.get(), 10);
        }
        for (var suit : List.of(one, several)) {
            charge(suit, 100_000);
        }

        var reachOne = wearer(helper, one);
        reachOne.tick();
        var reachSeveral = wearer(helper, several);
        reachSeveral.tick();

        double addedOne = blockReach(reachOne) - base(reachOne, true);
        double addedSeveral = blockReach(reachSeveral) - base(reachSeveral, true);
        helper.assertTrue(addedSeveral > addedOne, Component.literal("four reach upgrades reached no further than one"));
        helper.assertTrue(addedSeveral < GearworksConfig.ceiling(Stat.REACH),
                Component.literal("four reach upgrades added " + addedSeveral + ", over the Ceiling " + GearworksConfig.ceiling(Stat.REACH)));
        helper.succeed();
    }

    private static void raisedReach(GameTestHelper helper) {
        var upgrade = SuitFixtures.upgrade(GearContent.REACH_UPGRADE.get());
        helper.assertTrue(Math.abs(upgrade.amountAt(1) - upgrade.amountAt(0) - 0.5) < TOLERANCE,
                Component.literal("a Level adds " + (upgrade.amountAt(1) - upgrade.amountAt(0)) + " blocks before the Ceiling"));
        helper.assertValueEqual(upgrade.drawAt(1) - upgrade.drawAt(0), 2, Component.literal("the draw a Level adds"));

        var fresh = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.REACH_UPGRADE.get());
        var raised = suitWith(GearContent.BATTERY_UPGRADE.get());
        addAtLevel(raised, GearContent.REACH_UPGRADE.get(), 5);
        for (var suit : List.of(fresh, raised)) {
            charge(suit, 1000);
        }
        var freshPlayer = wearer(helper, fresh);
        freshPlayer.tick();
        var raisedPlayer = wearer(helper, raised);
        raisedPlayer.tick();

        helper.assertTrue(blockReach(raisedPlayer) > blockReach(freshPlayer), Component.literal("Level 5 reached no further than Level 0"));
        helper.assertValueEqual(Suit.energy(fresh) - Suit.energy(raised), 5 * 2, Component.literal("the FE Level 5 draws a tick over Level 0"));
        helper.succeed();
    }

    private static void crafts(GameTestHelper helper) {
        var iron = new ItemStack(Items.IRON_INGOT);
        var pearl = new ItemStack(Items.ENDER_PEARL);
        var input = CraftingInput.of(3, 3, List.of(
                iron, pearl, iron,
                pearl, new ItemStack(Items.PISTON), pearl,
                iron, new ItemStack(Items.REDSTONE), iron));
        var level = helper.getLevel();
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        helper.assertTrue(recipe.isPresent(), Component.literal("no recipe makes the reach upgrade"));
        helper.assertTrue(recipe.get().value().assemble(input).is(GearContent.REACH_UPGRADE.get()), Component.literal("the recipe makes something else"));
        helper.succeed();
    }

    private static void levelCost(GameTestHelper helper) {
        var cost = LevelCost.of(GearContent.REACH_UPGRADE.get().builtInRegistryHolder());
        helper.assertTrue(cost != null, Component.literal("the reach upgrade has no Level cost"));
        for (int level : List.of(0, 5)) {
            var materials = cost.at(level).materials();
            helper.assertTrue(materials.stream().anyMatch(material -> material.what().equals(Tags.Items.ENDER_PEARLS)),
                    Component.literal("Level " + level + " costs no ender pearls"));
        }
        helper.assertTrue(cost.at(0).energy() > 0, Component.literal("the first Level costs no FE"));
        helper.succeed();
    }

    /** Both of the player's reaches are vanilla's base plus those blocks. */
    private static void assertReach(GameTestHelper helper, Player player, double added, String when) {
        helper.assertTrue(Math.abs(blockReach(player) - base(player, true) - added) < TOLERANCE,
                Component.literal("block reach " + when + ": " + blockReach(player) + ", not " + (base(player, true) + added)));
        helper.assertTrue(Math.abs(entityReach(player) - base(player, false) - added) < TOLERANCE,
                Component.literal("entity reach " + when + ": " + entityReach(player) + ", not " + (base(player, false) + added)));
    }

    private static double blockReach(Player player) {
        return player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
    }

    private static double entityReach(Player player) {
        return player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
    }

    private static double base(Player player, boolean block) {
        return player.getAttributeBaseValue(block ? Attributes.BLOCK_INTERACTION_RANGE : Attributes.ENTITY_INTERACTION_RANGE);
    }
}
