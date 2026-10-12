// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import io.github._5thlayer.gearworks.client.EnergyHudLayout.Corner;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Each player's own choices: whether the HUD shows the worn suit's FE, and in which corner. */
final class GearworksClientConfig {

    static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue SHOW_ENERGY;
    private static final ModConfigSpec.EnumValue<Corner> ENERGY_CORNER;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("energyHud");
        SHOW_ENERGY = builder
                .comment("Show the worn suit's FE on the HUD while it holds a battery.")
                .define("show", true);
        ENERGY_CORNER = builder
                .comment("The screen corner the reading hugs.")
                .defineEnum("corner", Corner.TOP_LEFT);
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
}
