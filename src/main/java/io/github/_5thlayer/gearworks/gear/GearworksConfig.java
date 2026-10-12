// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The server's numbers that belong to a Stat rather than to an upgrade: an upgrade's own values are
 * in its data map entry.
 */
public final class GearworksConfig {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.DoubleValue SPEED_CEILING;

    static {
        var builder = new ModConfigSpec.Builder();
        SPEED_CEILING = builder
                .comment("The walking speed the suit's speed upgrades together approach and never reach, as a fraction",
                        "of base speed: 1.0 is twice as fast. Upgrades of one Stat sum, and the Ceiling holds for the sum.")
                .defineInRange("speedCeiling", 1.0, 0.0, 10.0);
        SPEC = builder.build();
    }

    private GearworksConfig() {
    }

    /** The speed Ceiling, or its default before the server's config has loaded, as on a client's tooltip. */
    public static double speedCeiling() {
        return SPEC.isLoaded() ? SPEED_CEILING.get() : SPEED_CEILING.getDefault();
    }
}
