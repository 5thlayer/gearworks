// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import java.util.List;

import io.github._5thlayer.gearworks.Gearworks;
import io.github._5thlayer.libworks.WorksTab;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

/** Gearworks' items are in libworks' Works tab, which every -works Module shares, and in no other. */
final class CreativeTabTests {

    private static final List<String> WORKS_TAB_ORDER = List.of(
            "gear_bench", "iron_gear", "diamond_gear", "netherite_gear", "battery_upgrade", "solar_upgrade", "speed_upgrade",
            "shield_upgrade", "jump_upgrade", "night_vision_upgrade");

    private CreativeTabTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("the_works_tab_holds_every_item_in_order", 1, CreativeTabTests::worksTabInOrder);
        tests.test("no_other_tab_holds_a_gearworks_item", 1, CreativeTabTests::noOtherTab);
    }

    private static void worksTabInOrder(GameTestHelper helper) {
        rebuild(helper);
        helper.assertValueEqual(ownItems(BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(WorksTab.KEY)), WORKS_TAB_ORDER,
                Component.literal("the Works tab's Gearworks items"));
        helper.succeed();
    }

    private static void noOtherTab(GameTestHelper helper) {
        rebuild(helper);
        // An empty Works tab would pass the loop below without Gearworks' items reaching any tab.
        helper.assertFalse(ownItems(BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(WorksTab.KEY)).isEmpty(),
                Component.literal("the Works tab shows no Gearworks item, so no tab was filled"));
        for (var entry : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            // The search tab lists every other tab's items.
            if (entry.getKey() == WorksTab.KEY || entry.getValue().getType() == CreativeModeTab.Type.SEARCH) {
                continue;
            }
            helper.assertValueEqual(ownItems(entry.getValue()), List.of(),
                    Component.literal("the Gearworks items of " + entry.getKey().identifier()));
        }
        helper.succeed();
    }

    private static void rebuild(GameTestHelper helper) {
        CreativeModeTabs.tryRebuildTabContents(helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
    }

    private static List<String> ownItems(CreativeModeTab tab) {
        return tab.getDisplayItems().stream()
                .map(ItemStack::getItem)
                .map(BuiltInRegistries.ITEM::getKey)
                .filter(id -> id.getNamespace().equals(Gearworks.MOD_ID))
                .map(Identifier::getPath)
                .toList();
    }
}
