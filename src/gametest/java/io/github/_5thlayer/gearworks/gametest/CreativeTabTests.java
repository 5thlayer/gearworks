// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import java.util.List;

import io.github._5thlayer.gearworks.Gearworks;
import io.github._5thlayer.libworks.testkit.WorksTabCheck;

/** Gearworks' items are in libworks' Works tab, which every -works Module shares, and in no other. */
final class CreativeTabTests {

    private static final List<String> WORKS_TAB_ORDER = List.of(
            "gear_bench", "iron_gear", "diamond_gear", "netherite_gear", "iron_bore_lance", "diamond_bore_lance", "netherite_bore_lance", "battery_upgrade",
            "solar_upgrade", "nuclear_upgrade", "kinetic_upgrade", "combustion_upgrade", "metabolic_upgrade", "speed_upgrade", "shield_upgrade", "jump_upgrade", "reach_upgrade", "night_vision_upgrade", "flight_upgrade",
            "boost_upgrade");

    private CreativeTabTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("the_works_tab_holds_every_item_in_order", 1,
                helper -> WorksTabCheck.inOrder(helper, Gearworks.MOD_ID, WORKS_TAB_ORDER));
        tests.test("no_other_tab_holds_a_gearworks_item", 1, helper -> WorksTabCheck.noOtherTab(helper, Gearworks.MOD_ID));
    }
}
