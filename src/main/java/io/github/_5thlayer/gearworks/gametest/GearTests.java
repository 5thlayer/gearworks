// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.GearFixtures.charge;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.energy;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.suitWith;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.upgrade;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.wearer;

import io.github._5thlayer.gearworks.gear.Gear;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearTicker;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Wearing the suit, filling it through NeoForge's energy capability, and its speed, which runs only
 * while the suit has the FE to draw.
 */
final class GearTests {

    private GearTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("the_suit_gives_an_iron_chestplates_armor", 20, GearTests::ironArmor);
        tests.test("the_suit_is_worn_in_the_chest_slot_only", 1, GearTests::chestOnly);
        tests.test("using_the_suit_in_hand_puts_it_on", 1, GearTests::equipsOnUse);
        tests.test("a_charger_fills_the_suit_through_the_energy_capability", 1, GearTests::charges);
        tests.test("a_suit_without_a_battery_takes_no_fe", 1, GearTests::noBattery);
        tests.test("speed_applies_while_the_suit_has_fe", 20, GearTests::speedWithEnergy);
        tests.test("speed_never_applies_to_an_empty_suit", 20, GearTests::noSpeedWhenEmpty);
        tests.test("speed_draws_fe_while_walking_and_stops_when_the_suit_is_empty", 40, GearTests::speedDrains);
        tests.test("taking_the_suit_off_takes_its_speed_away", 20, GearTests::speedLeavesWithTheSuit);
    }

    private static void ironArmor(GameTestHelper helper) {
        var suited = wearer(helper, new ItemStack(GearContent.IRON_GEAR.get()));
        var plated = helper.makeMockPlayer(GameType.SURVIVAL);
        plated.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        helper.onEachTick(() -> {
            suited.tick();
            plated.tick();
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(plated.getArmorValue() > 0, Component.literal("the iron chestplate gave no armor yet"));
            helper.assertValueEqual(suited.getArmorValue(), plated.getArmorValue(), Component.literal("armor"));
        });
    }

    private static void chestOnly(GameTestHelper helper) {
        var equippable = new ItemStack(GearContent.IRON_GEAR.get()).get(DataComponents.EQUIPPABLE);
        helper.assertTrue(equippable != null, Component.literal("the suit is not wearable"));
        helper.assertValueEqual(equippable.slot(), EquipmentSlot.CHEST, Component.literal("the suit's slot"));
        helper.succeed();
    }

    private static void equipsOnUse(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(GearContent.IRON_GEAR.get()));

        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(Gear.is(player.getItemBySlot(EquipmentSlot.CHEST)), Component.literal("the suit is not worn"));
        helper.assertTrue(player.getMainHandItem().isEmpty(), Component.literal("the suit is still in hand"));
        helper.succeed();
    }

    private static void charges(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get());
        int capacity = (int) upgrade(GearContent.BATTERY_UPGRADE.get()).amount();
        helper.assertValueEqual(energy(suit).getCapacityAsLong(), (long) capacity, Component.literal("capacity"));

        helper.assertValueEqual(charge(suit, 1000), 1000, Component.literal("FE taken"));
        helper.assertValueEqual(Gear.energy(suit), 1000, Component.literal("FE held"));

        helper.assertValueEqual(charge(suit, capacity), capacity - 1000, Component.literal("FE taken when nearly full"));
        helper.assertValueEqual(energy(suit).getAmountAsLong(), (long) capacity, Component.literal("FE held when full"));
        helper.succeed();
    }

    private static void noBattery(GameTestHelper helper) {
        var suit = suitWith(GearContent.SOLAR_UPGRADE.get());
        helper.assertValueEqual(charge(suit, 1000), 0, Component.literal("FE taken"));
        helper.succeed();
    }

    private static void speedWithEnergy(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SPEED_UPGRADE.get());
        charge(suit, 1000);
        var player = wearer(helper, suit);
        double base = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.onEachTick(player::tick);
        helper.succeedWhen(() -> {
            helper.assertTrue(hasSpeed(player), Component.literal("no speed while the suit has FE"));
            helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > base, Component.literal("walking speed did not rise"));
            // Standing still draws nothing.
            helper.assertValueEqual(Gear.energy(suit), 1000, Component.literal("FE held while standing"));
        });
    }

    private static void noSpeedWhenEmpty(GameTestHelper helper) {
        var player = wearer(helper, suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SPEED_UPGRADE.get()));
        helper.onEachTick(player::tick);
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(hasSpeed(player), Component.literal("speed on an empty suit"));
            helper.succeed();
        });
    }

    private static void speedDrains(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SPEED_UPGRADE.get());
        int draw = upgrade(GearContent.SPEED_UPGRADE.get()).draw();
        helper.assertTrue(draw > 0, Component.literal("the speed upgrade draws no FE"));
        charge(suit, 3 * draw);
        var player = wearer(helper, suit);
        helper.onEachTick(() -> {
            player.setDeltaMovement(new Vec3(0.1, 0, 0));
            player.tick();
        });
        helper.succeedWhen(() -> {
            helper.assertValueEqual(Gear.energy(suit), 0, Component.literal("FE left after walking"));
            helper.assertFalse(hasSpeed(player), Component.literal("speed on an empty suit"));
        });
    }

    private static void speedLeavesWithTheSuit(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SPEED_UPGRADE.get());
        charge(suit, 1000);
        var player = wearer(helper, suit);
        player.tick();
        helper.assertTrue(hasSpeed(player), Component.literal("no speed while the suit has FE"));
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        helper.onEachTick(player::tick);
        helper.succeedWhen(() -> helper.assertFalse(hasSpeed(player), Component.literal("speed kept without the suit")));
    }

    private static boolean hasSpeed(Player player) {
        return player.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(GearTicker.SPEED);
    }
}
