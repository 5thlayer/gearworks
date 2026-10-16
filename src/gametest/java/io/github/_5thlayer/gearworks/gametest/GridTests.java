// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.SuitFixtures.charge;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.suitWith;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.upgrade;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.wearer;

import java.util.List;

import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearGridMenu;
import io.github._5thlayer.gearworks.grid.Placement;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

/**
 * The Gear Grid's menu places, moves and removes upgrades, refusing overlaps and the edge, and the
 * layout survives a save and reload. A click on a cell is the menu button the screen sends for it.
 */
final class GridTests {

    private GridTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("a_click_places_the_carried_upgrade", 1, GridTests::places);
        tests.test("a_placement_over_another_upgrade_is_refused", 1, GridTests::refusesOverlap);
        tests.test("a_placement_over_the_edge_is_refused", 1, GridTests::refusesEdge);
        tests.test("a_click_picks_an_upgrade_up_and_another_places_it_elsewhere", 1, GridTests::moves);
        tests.test("moving_the_only_battery_keeps_its_fe", 1, GridTests::movingABatteryKeepsItsFe);
        tests.test("a_closed_grid_keeps_no_more_fe_than_its_batteries_hold", 1, GridTests::closingDropsFeNoBatteryHolds);
        tests.test("a_datapacks_upgrade_fits_the_grid_with_no_code", 1, GridTests::datapackUpgrade);
        tests.test("the_layout_survives_a_save_and_reload", 1, GridTests::savesAndLoads);
        tests.test("a_shift_click_moves_an_upgrade_from_the_grid_to_the_inventory", 1, GridTests::shiftClickTakesOut);
        tests.test("a_shift_click_leaves_an_upgrade_in_the_grid_when_the_inventory_is_full", 1, GridTests::shiftClickKeepsWhenFull);
        tests.test("a_click_with_the_same_upgrade_in_hand_stacks_it_there", 1, GridTests::stacksIntoHand);
        tests.test("a_click_with_the_same_upgrade_at_another_level_in_hand_does_not_stack", 1, GridTests::otherLevelDoesNotStack);
        tests.test("a_suit_holds_one_night_vision_upgrade", 1, GridTests::oneNightVision);
    }

    private static void places(GameTestHelper helper) {
        var player = wearer(helper, suitWith());
        var menu = grid(player);
        menu.setCarried(new ItemStack(GearContent.SPEED_UPGRADE.get(), 2));

        helper.assertTrue(menu.clickMenuButton(player, cell(1, 1)), Component.literal("the click placed nothing"));

        var speed = upgrade(GearContent.SPEED_UPGRADE.get());
        helper.assertValueEqual(Suit.grid(menu.suit()).placements(),
                List.of(new Placement<>(1, 1, speed.width(), speed.height(), new ItemStackTemplate(GearContent.SPEED_UPGRADE.get()))),
                Component.literal("the layout"));
        helper.assertValueEqual(menu.getCarried().getCount(), 1, Component.literal("upgrades left in hand"));
        helper.succeed();
    }

    private static void refusesOverlap(GameTestHelper helper) {
        var player = wearer(helper, suitWith());
        var menu = grid(player);
        menu.setCarried(new ItemStack(GearContent.SPEED_UPGRADE.get(), 2));
        menu.clickMenuButton(player, cell(1, 1));

        // The clicked cell is free, but a speed upgrade's two by two from there would cover (1, 1).
        helper.assertFalse(menu.clickMenuButton(player, cell(0, 0)), Component.literal("an overlapping upgrade was placed"));
        helper.assertValueEqual(Suit.grid(menu.suit()).placements().size(), 1, Component.literal("upgrades placed"));
        helper.assertValueEqual(menu.getCarried().getCount(), 1, Component.literal("upgrades left in hand"));
        helper.succeed();
    }

    private static void refusesEdge(GameTestHelper helper) {
        var player = wearer(helper, suitWith());
        var menu = grid(player);
        menu.setCarried(new ItemStack(GearContent.BATTERY_UPGRADE.get()));
        var battery = upgrade(GearContent.BATTERY_UPGRADE.get());
        int x = menu.gridWidth() - battery.width() + 1;
        int y = menu.gridHeight() - battery.height() + 1;

        helper.assertFalse(menu.clickMenuButton(player, cell(x, y)), Component.literal("an upgrade was placed over the edge"));
        helper.assertTrue(Suit.grid(menu.suit()).placements().isEmpty(), Component.literal("the grid is not empty"));
        helper.succeed();
    }

    private static void moves(GameTestHelper helper) {
        var player = wearer(helper, suitWith(GearContent.SOLAR_UPGRADE.get()));
        var menu = grid(player);

        helper.assertTrue(menu.clickMenuButton(player, cell(0, 0)), Component.literal("the click picked nothing up"));
        helper.assertTrue(menu.getCarried().is(GearContent.SOLAR_UPGRADE.get()), Component.literal("the upgrade is not in hand"));
        helper.assertTrue(Suit.grid(menu.suit()).placements().isEmpty(), Component.literal("the upgrade is still in the grid"));

        helper.assertTrue(menu.clickMenuButton(player, cell(5, 2)), Component.literal("the click placed nothing"));
        helper.assertTrue(menu.getCarried().isEmpty(), Component.literal("the upgrade is still in hand"));
        helper.assertTrue(Suit.grid(menu.suit()).at(5, 2).isPresent(), Component.literal("the upgrade is not where it was placed"));
        helper.succeed();
    }

    private static void movingABatteryKeepsItsFe(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get());
        charge(suit, 1000);
        var player = wearer(helper, suit);
        var menu = grid(player);

        menu.clickMenuButton(player, GearGridMenu.cellButton(0, 0));
        menu.clickMenuButton(player, GearGridMenu.cellButton(2, 0));

        helper.assertValueEqual(Suit.energy(menu.suit()), 1000, Component.literal("FE held after the move"));
        helper.succeed();
    }

    private static void closingDropsFeNoBatteryHolds(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.BATTERY_UPGRADE.get());
        int perBattery = (int) upgrade(GearContent.BATTERY_UPGRADE.get()).amount();
        charge(suit, 2 * perBattery);
        var player = wearer(helper, suit);
        var menu = grid(player);

        menu.clickMenuButton(player, GearGridMenu.cellButton(0, 0));
        menu.removed(player);

        helper.assertValueEqual(Suit.energy(suit), perBattery, Component.literal("FE held by one battery"));
        helper.succeed();
    }

    private static void datapackUpgrade(GameTestHelper helper) {
        // gametest_pack's data map makes a redstone block a one-cell battery of 1234 FE.
        var player = wearer(helper, suitWith());
        var menu = grid(player);
        menu.setCarried(new ItemStack(Items.REDSTONE_BLOCK));

        helper.assertTrue(menu.clickMenuButton(player, cell(8, 2)), Component.literal("the redstone block did not fit"));
        helper.assertValueEqual(Suit.capacity(menu.suit()), 1234, Component.literal("capacity"));
        helper.succeed();
    }

    private static void savesAndLoads(GameTestHelper helper) {
        var suit = suitWith(GearContent.SPEED_UPGRADE.get(), GearContent.BATTERY_UPGRADE.get(), GearContent.SOLAR_UPGRADE.get());
        charge(suit, 500);
        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);

        var saved = ItemStack.CODEC.encodeStart(ops, suit).getOrThrow();
        var loaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();

        helper.assertValueEqual(Suit.grid(loaded).placements(), Suit.grid(suit).placements(), Component.literal("the layout"));
        helper.assertValueEqual(Suit.grid(loaded).placements().size(), 3, Component.literal("upgrades placed"));
        helper.assertValueEqual(Suit.energy(loaded), 500, Component.literal("FE held"));
        helper.succeed();
    }

    private static void shiftClickTakesOut(GameTestHelper helper) {
        var player = wearer(helper, suitWith(GearContent.SOLAR_UPGRADE.get()));
        var menu = grid(player);

        helper.assertTrue(menu.clickMenuButton(player, GearGridMenu.shiftCellButton(0, 0)), Component.literal("the shift-click took nothing"));

        helper.assertTrue(Suit.grid(menu.suit()).placements().isEmpty(), Component.literal("the upgrade is still in the grid"));
        helper.assertTrue(menu.getCarried().isEmpty(), Component.literal("the upgrade went to the hand"));
        helper.assertValueEqual(player.getInventory().countItem(GearContent.SOLAR_UPGRADE.get()), 1, Component.literal("solar upgrades in the inventory"));
        helper.succeed();
    }

    private static void shiftClickKeepsWhenFull(GameTestHelper helper) {
        var player = wearer(helper, suitWith(GearContent.SOLAR_UPGRADE.get()));
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getNonEquipmentItems().size(); i++) {
            inventory.setItem(i, new ItemStack(Items.STONE, 64));
        }
        var menu = grid(player);

        helper.assertFalse(menu.clickMenuButton(player, GearGridMenu.shiftCellButton(0, 0)), Component.literal("the shift-click took the upgrade"));

        helper.assertValueEqual(Suit.grid(menu.suit()).placements().size(), 1, Component.literal("upgrades in the grid"));
        helper.succeed();
    }

    private static void stacksIntoHand(GameTestHelper helper) {
        var player = wearer(helper, suitWith(GearContent.SOLAR_UPGRADE.get()));
        var menu = grid(player);
        menu.setCarried(new ItemStack(GearContent.SOLAR_UPGRADE.get(), 3));

        helper.assertTrue(menu.clickMenuButton(player, cell(0, 0)), Component.literal("the click did nothing"));

        helper.assertTrue(Suit.grid(menu.suit()).placements().isEmpty(), Component.literal("the upgrade is still in the grid"));
        helper.assertValueEqual(menu.getCarried().getCount(), 4, Component.literal("upgrades in hand"));
        helper.succeed();
    }

    private static void otherLevelDoesNotStack(GameTestHelper helper) {
        var player = wearer(helper, suitWith(GearContent.SOLAR_UPGRADE.get()));
        var menu = grid(player);
        var raised = new ItemStack(GearContent.SOLAR_UPGRADE.get(), 3);
        raised.set(GearContent.LEVEL.get(), 2);
        menu.setCarried(raised);

        helper.assertFalse(menu.clickMenuButton(player, cell(0, 0)), Component.literal("the click did something"));

        helper.assertValueEqual(Suit.grid(menu.suit()).placements().size(), 1, Component.literal("upgrades in the grid"));
        helper.assertValueEqual(menu.getCarried().getCount(), 3, Component.literal("upgrades in hand"));
        helper.succeed();
    }

    private static void oneNightVision(GameTestHelper helper) {
        var player = wearer(helper, suitWith(GearContent.NIGHT_VISION_UPGRADE.get()));
        // The first slot past the locked suit is the inventory's first main slot.
        player.getInventory().setItem(9, new ItemStack(GearContent.NIGHT_VISION_UPGRADE.get()));
        var menu = grid(player);
        menu.setCarried(new ItemStack(GearContent.NIGHT_VISION_UPGRADE.get()));

        helper.assertFalse(menu.clickMenuButton(player, cell(4, 1)), Component.literal("a click placed a second night vision upgrade"));
        // Menu slot 1, past the locked suit, stands over inventory slot 9.
        menu.quickMoveStack(player, 1);

        helper.assertValueEqual(Suit.grid(menu.suit()).placements().size(), 1, Component.literal("upgrades in the grid"));
        helper.assertValueEqual(player.getInventory().getItem(9).getCount(), 1, Component.literal("night vision upgrades left in the inventory"));
        helper.succeed();
    }

    private static GearGridMenu grid(Player player) {
        return new GearGridMenu(1, player.getInventory());
    }

    private static int cell(int x, int y) {
        return GearGridMenu.cellButton(x, y);
    }
}
