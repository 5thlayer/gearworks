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
    private static final ModConfigSpec.DoubleValue JUMP_CEILING;
    private static final ModConfigSpec.DoubleValue SHIELD_CEILING;

    static {
        var builder = new ModConfigSpec.Builder();
        SPEED_CEILING = builder
                .comment("The walking speed the suit's speed upgrades together approach and never reach, as a fraction",
                        "of base speed: 1.0 is twice as fast. Upgrades of one Stat sum, and the Ceiling holds for the sum.")
                .defineInRange("speedCeiling", 1.0, 0.0, 10.0);
        JUMP_CEILING = builder
                .comment("The jump strength the suit's jump upgrades together approach and never reach, as a fraction",
                        "of base strength. 0.5 keeps a jump under three blocks, so landing on flat ground does no harm.")
                .defineInRange("jumpCeiling", 0.5, 0.0, 10.0);
        SHIELD_CEILING = builder
                .comment("The fraction of each hit the suit's shield upgrades together approach and never reach:",
                        "1.0 would absorb every hit whole.")
                .defineInRange("shieldCeiling", 0.8, 0.0, 1.0);
        SPEC = builder.build();
    }

    private GearworksConfig() {
    }

    /** The speed Ceiling, or its default before the server's config has loaded, as on a client's tooltip. */
    public static double speedCeiling() {
        return valueOf(SPEED_CEILING);
    }

    /** The jump Ceiling, or its default before the server's config has loaded. */
    public static double jumpCeiling() {
        return valueOf(JUMP_CEILING);
    }

    /** The shield Ceiling, or its default before the server's config has loaded. */
    public static double shieldCeiling() {
        return valueOf(SHIELD_CEILING);
    }

    private static double valueOf(ModConfigSpec.DoubleValue ceiling) {
        return SPEC.isLoaded() ? ceiling.get() : ceiling.getDefault();
    }
}
