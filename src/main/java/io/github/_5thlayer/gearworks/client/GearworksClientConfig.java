// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import io.github._5thlayer.gearworks.client.EnergyHudLayout.Corner;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Each player's own choices: whether the HUD shows the worn suit's FE, in which corner, and how far from it. */
final class GearworksClientConfig {

    static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue SHOW_ENERGY;
    private static final ModConfigSpec.EnumValue<Corner> ENERGY_CORNER;
    private static final ModConfigSpec.IntValue ENERGY_OFFSET_X;
    private static final ModConfigSpec.IntValue ENERGY_OFFSET_Y;

    /** The farthest the reading can be pushed from its corner, in GUI-scaled pixels. */
    private static final int MAX_OFFSET = 4096;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("energyHud");
        SHOW_ENERGY = builder
                .comment("Show the worn suit's FE on the HUD while it holds a battery.")
                .define("show", true);
        ENERGY_CORNER = builder
                .comment("The screen corner the reading hugs.")
                .defineEnum("corner", Corner.TOP_LEFT);
        ENERGY_OFFSET_X = builder
                .comment("How far the reading is pushed away from its corner's side, in GUI-scaled pixels,"
                        + " to clear other mods' HUD elements.")
                .defineInRange("offsetX", 0, 0, MAX_OFFSET);
        ENERGY_OFFSET_Y = builder
                .comment("How far the reading is pushed away from its corner's top or bottom, in GUI-scaled pixels,"
                        + " to clear other mods' HUD elements.")
                .defineInRange("offsetY", 0, 0, MAX_OFFSET);
        builder.pop();
        SPEC = builder.build();
    }

    private GearworksClientConfig() {
    }

    /** Whether the HUD shows the suit's FE, or the default before the client's config has loaded. */
    static boolean showEnergy() {
        return SPEC.isLoaded() ? SHOW_ENERGY.get() : SHOW_ENERGY.getDefault();
    }

    /** The corner of the HUD's energy reading, or the default before the client's config has loaded. */
    static Corner energyCorner() {
        return SPEC.isLoaded() ? ENERGY_CORNER.get() : ENERGY_CORNER.getDefault();
    }

    /** How far the reading is pushed from its corner's side, or the default before the config has loaded. */
    static int energyOffsetX() {
        return SPEC.isLoaded() ? ENERGY_OFFSET_X.get() : ENERGY_OFFSET_X.getDefault();
    }

    /** How far the reading is pushed from its corner's top or bottom, or the default before the config has loaded. */
    static int energyOffsetY() {
        return SPEC.isLoaded() ? ENERGY_OFFSET_Y.get() : ENERGY_OFFSET_Y.getDefault();
    }
}
