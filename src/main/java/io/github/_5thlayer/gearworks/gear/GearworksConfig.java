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
    private static final ModConfigSpec.IntValue NIGHT_VISION_FLOOR;
    private static final ModConfigSpec.IntValue FLIGHT_FLOOR;

    static {
        var builder = new ModConfigSpec.Builder();
        SPEED_CEILING = builder
                .comment("The walking speed the suit's speed upgrades together approach and never reach, as a fraction",
                        "of base speed: 1.0 is twice as fast. Upgrades of one Stat sum, and the Ceiling holds for the sum.")
                .defineInRange("speedCeiling", 1.0, 0.0, 10.0);
        JUMP_CEILING = builder
                .comment("The jump strength the suit's jump upgrades together approach and never reach, as a fraction",
                        "of base strength. The default, 0.5, keeps a jump under three blocks, so landing on flat ground does no harm.")
                .defineInRange("jumpCeiling", 0.5, 0.0, 10.0);
        SHIELD_CEILING = builder
                .comment("The fraction of each hit the suit's shield upgrades together approach and never reach:",
                        "1.0 would absorb every hit whole.")
                .defineInRange("shieldCeiling", 0.8, 0.0, 1.0);
        NIGHT_VISION_FLOOR = builder
                .comment("The FE per tick a night vision upgrade's draw approaches as it levels, and is never pushed under.")
                .defineInRange("nightVisionFloor", 1, 0, 1_000_000);
        FLIGHT_FLOOR = builder
                .comment("The FE per tick in the air a flight upgrade's draw approaches as it levels, and is never pushed under.",
                        "Over 0, so flight is never free.")
                .defineInRange("flightFloor", 20, 1, 1_000_000);
        SPEC = builder.build();
    }

    private GearworksConfig() {
    }

    /**
     * The Ceiling of a Stat that has one, or its default before the server's config has loaded, as on a
     * client's tooltip. A Stat without a Ceiling grows linearly (ADR 0003) and has none to ask for.
     */
    public static double ceiling(Stat stat) {
        var ceiling = switch (stat) {
            case SPEED -> SPEED_CEILING;
            case JUMP -> JUMP_CEILING;
            case SHIELD -> SHIELD_CEILING;
            default -> throw new IllegalArgumentException(stat + " has no Ceiling");
        };
        return SPEC.isLoaded() ? ceiling.get() : ceiling.getDefault();
    }

    /**
     * The Floor of a Stat whose draw falls as it levels, or its default before the server's config has
     * loaded; a Stat whose draw rises has none to ask for.
     */
    public static int floor(Stat stat) {
        var floor = switch (stat) {
            case NIGHT_VISION -> NIGHT_VISION_FLOOR;
            case FLIGHT -> FLIGHT_FLOOR;
            default -> throw new IllegalArgumentException(stat + " has no Floor");
        };
        return SPEC.isLoaded() ? floor.get() : floor.getDefault();
    }
}
