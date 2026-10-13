// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.GearFixtures.addAtLevel;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.charge;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.tierWith;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.wearer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github._5thlayer.gearworks.gear.Gear;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearUpgrade;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.GameType;

/**
 * The three tiers: each gives the armor of its chestplate, and each is made from the one below by its
 * recipe, keeping every upgrade where it sits, with its Level, and the suit's FE.
 */
final class TierTests {

    private TierTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("diamond_gear_gives_a_diamond_chestplates_armor", 20,
                helper -> sameArmor(helper, GearContent.DIAMOND_GEAR.get(), Items.DIAMOND_CHESTPLATE));
        tests.test("netherite_gear_gives_a_netherite_chestplates_armor", 20,
                helper -> sameArmor(helper, GearContent.NETHERITE_GEAR.get(), Items.NETHERITE_CHESTPLATE));
        tests.test("each_tier_has_a_larger_grid_than_the_one_below", 1, TierTests::largerGrids);
        tests.test("crafting_diamond_gear_from_iron_gear_keeps_its_layout", 1, TierTests::craftsDiamond);
        tests.test("smithing_netherite_gear_from_diamond_gear_keeps_its_layout", 1, TierTests::smithsNetherite);
    }

    private static void sameArmor(GameTestHelper helper, Item suit, Item chestplate) {
        var suited = wearer(helper, new ItemStack(suit));
        var plated = helper.makeMockPlayer(GameType.SURVIVAL);
        plated.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chestplate));
        helper.onEachTick(() -> {
            suited.tick();
            plated.tick();
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(plated.getArmorValue() > 0, Component.literal("the chestplate gave no armor yet"));
            helper.assertValueEqual(suited.getArmorValue(), plated.getArmorValue(), Component.literal("armor"));
            helper.assertValueEqual(suited.getAttributeValue(Attributes.ARMOR_TOUGHNESS), plated.getAttributeValue(Attributes.ARMOR_TOUGHNESS), Component.literal("armor toughness"));
        });
    }

    private static void largerGrids(GameTestHelper helper) {
        int iron = cells(new ItemStack(GearContent.IRON_GEAR.get()));
        int diamond = cells(new ItemStack(GearContent.DIAMOND_GEAR.get()));
        int netherite = cells(new ItemStack(GearContent.NETHERITE_GEAR.get()));
        helper.assertTrue(diamond > iron, Component.literal("diamond gear's grid is no larger than iron's"));
        helper.assertTrue(netherite > diamond, Component.literal("netherite gear's grid is no larger than diamond's"));
        helper.succeed();
    }

    private static int cells(ItemStack suit) {
        var grid = Gear.grid(suit);
        return grid.width() * grid.height();
    }

    private static void craftsDiamond(GameTestHelper helper) {
        var iron = laidOut(GearContent.IRON_GEAR.get());
        var items = new ArrayList<ItemStack>(Collections.nCopies(8, new ItemStack(Items.DIAMOND)));
        items.add(4, iron);
        var input = CraftingInput.of(3, 3, List.copyOf(items));
        var level = helper.getLevel();
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        helper.assertTrue(recipe.isPresent(), Component.literal("no recipe makes diamond gear from iron gear"));

        var diamond = recipe.get().value().assemble(input);

        helper.assertTrue(diamond.is(GearContent.DIAMOND_GEAR.get()), Component.literal("the recipe makes " + diamond));
        assertKeptLayout(helper, iron, diamond);
        helper.succeed();
    }

    private static void smithsNetherite(GameTestHelper helper) {
        var diamond = laidOut(GearContent.DIAMOND_GEAR.get());
        var input = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), diamond, new ItemStack(Items.NETHERITE_INGOT));
        var level = helper.getLevel();
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level);
        helper.assertTrue(recipe.isPresent(), Component.literal("no recipe makes netherite gear from diamond gear"));

        var netherite = recipe.get().value().assemble(input);

        helper.assertTrue(netherite.is(GearContent.NETHERITE_GEAR.get()), Component.literal("the recipe makes " + netherite));
        assertKeptLayout(helper, diamond, netherite);
        helper.succeed();
    }

    /** A suit of that tier holding a battery at Level 3, a solar and a speed upgrade, and some FE. */
    private static ItemStack laidOut(Item tier) {
        var suit = tierWith(tier, GearContent.SOLAR_UPGRADE.get(), GearContent.SPEED_UPGRADE.get());
        addAtLevel(suit, GearContent.BATTERY_UPGRADE.get(), 3);
        charge(suit, 1234);
        return suit;
    }

    private static void assertKeptLayout(GameTestHelper helper, ItemStack before, ItemStack after) {
        helper.assertValueEqual(Gear.grid(after).placements(), Gear.grid(before).placements(), Component.literal("the layout"));
        var battery = Gear.grid(after).placements().stream()
                .filter(placement -> placement.item().is(GearContent.BATTERY_UPGRADE.get())).findFirst().orElseThrow().item();
        helper.assertValueEqual(GearUpgrade.level(battery), 3, Component.literal("the battery's Level"));
        helper.assertValueEqual(Gear.energy(after), 1234, Component.literal("the suit's FE"));
    }
}
