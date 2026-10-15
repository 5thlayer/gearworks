// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The server's numbers that no data map entry holds: those that belong to a Stat rather than to an
 * upgrade, whose own values are in its data map entry, and the Bore Lance's costs and FE.
 */
public final class GearworksConfig {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.DoubleValue SPEED_CEILING;
    private static final ModConfigSpec.DoubleValue JUMP_CEILING;
    private static final ModConfigSpec.DoubleValue SHIELD_CEILING;
    private static final ModConfigSpec.DoubleValue FLIGHT_CEILING;
    private static final ModConfigSpec.IntValue NIGHT_VISION_FLOOR;
    private static final ModConfigSpec.IntValue FLIGHT_FLOOR;
    private static final ModConfigSpec.IntValue LUNGE_BORING_COST;
    private static final ModConfigSpec.IntValue NETHERITE_LANCE_CAPACITY;
    private static final ModConfigSpec.IntValue NETHERITE_LANCE_LUNGE_ENERGY;
    private static final ModConfigSpec.IntValue NETHERITE_LANCE_BORE_ENERGY;
    private static final ModConfigSpec.IntValue NETHERITE_LANCE_LUNGE_BORING_ENERGY;
    private static final ModConfigSpec.IntValue NETHERITE_LANCE_CHARGE_RATE;
    private static final ModConfigSpec.DoubleValue NETHERITE_LANCE_CHARGE_RESERVE;
    private static final ModConfigSpec.DoubleValue BOOST_CEILING;
    private static final ModConfigSpec.IntValue BOOST_FLOOR;
    private static final ModConfigSpec.IntValue BOOST_COOLDOWN_FLOOR;
    private static final ModConfigSpec.IntValue BOOST_TICKS;

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
        FLIGHT_CEILING = builder
                .comment("The flying speed the suit's flight upgrade approaches as it levels and never reaches, as a fraction",
                        "of vanilla's creative flying speed: 1.0 is creative speed, and sprinting still doubles it.")
                .defineInRange("flightCeiling", 2.0, 0.1, 10.0);
        NIGHT_VISION_FLOOR = builder
                .comment("The FE per tick a night vision upgrade's draw approaches as it levels, and is never pushed under.")
                .defineInRange("nightVisionFloor", 1, 0, 1_000_000);
        FLIGHT_FLOOR = builder
                .comment("The FE per tick in the air a flight upgrade's draw approaches as it levels, and is never pushed under.",
                        "Over 0, so flight is never free.")
                .defineInRange("flightFloor", 20, 1, 1_000_000);
        LUNGE_BORING_COST = builder
                .comment("The durability a Bore Lance pays for each block its Lunge breaks, where mining one costs 1.")
                .defineInRange("lungeBoringCost", 10, 0, 1_000);
        NETHERITE_LANCE_CAPACITY = builder
                .comment("The FE the netherite Bore Lance holds.")
                .defineInRange("netheriteLanceCapacity", 1_000_000, 1, Integer.MAX_VALUE);
        NETHERITE_LANCE_LUNGE_ENERGY = builder
                .comment("The FE the netherite Bore Lance pays for a lunge, for each level of its Lunge.")
                .defineInRange("netheriteLanceLungeEnergy", 2_000, 0, 1_000_000);
        NETHERITE_LANCE_BORE_ENERGY = builder
                .comment("The FE the netherite Bore Lance pays for each block it mines in a Bore.")
                .defineInRange("netheriteLanceBoreEnergy", 50, 0, 1_000_000);
        NETHERITE_LANCE_LUNGE_BORING_ENERGY = builder
                .comment("The FE the netherite Bore Lance pays for each block its Lunge breaks.")
                .defineInRange("netheriteLanceLungeBoringEnergy", 500, 0, 1_000_000);
        NETHERITE_LANCE_CHARGE_RATE = builder
                .comment("The FE per tick a worn Suit gives a netherite Bore Lance in either of the player's hands.")
                .defineInRange("netheriteLanceChargeRate", 1_000, 0, 1_000_000);
        NETHERITE_LANCE_CHARGE_RESERVE = builder
                .comment("The fraction of what its batteries hold that a Suit keeps for itself: it charges a lance",
                        "only while it holds more, and never goes under it doing so.")
                .defineInRange("netheriteLanceChargeReserve", 0.25, 0.0, 1.0);
        BOOST_CEILING = builder
                .comment("The speed in blocks a tick a Boost's thrust approaches and never reaches as its upgrade levels.",
                        "A vanilla rocket pulls a glider toward 1.5.")
                .defineInRange("boostCeiling", 2.5, 0.0, 10.0);
        BOOST_FLOOR = builder
                .comment("The FE a boost upgrade spends on each Boost approaches as it levels, and is never pushed under.",
                        "Over 0, so a Boost is never free.")
                .defineInRange("boostFloor", 200, 1, 1_000_000);
        BOOST_COOLDOWN_FLOOR = builder
                .comment("The ticks a boost upgrade's Cooldown approaches as it levels, and is never pushed under.",
                        "A Cooldown is never shorter than the thrust, whatever this says, so two Boosts never overlap.")
                .defineInRange("boostCooldownFloor", 25, 1, 1_000_000);
        BOOST_TICKS = builder
                .comment("The ticks each Boost thrusts for, whatever the Level. A vanilla rocket of flight duration 1 thrusts for 20 to 31.")
                .defineInRange("boostTicks", 25, 1, 200);
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
            case FLIGHT -> FLIGHT_CEILING;
            case BOOST -> BOOST_CEILING;
            default -> throw new IllegalArgumentException(stat + " has no Ceiling");
        };
        return value(ceiling);
    }

    /**
     * The Floor of a Stat whose draw falls as it levels, or its default before the server's config has
     * loaded; a Stat whose draw rises has none to ask for.
     */
    public static int floor(Stat stat) {
        var floor = switch (stat) {
            case NIGHT_VISION -> NIGHT_VISION_FLOOR;
            case FLIGHT -> FLIGHT_FLOOR;
            case BOOST -> BOOST_FLOOR;
            default -> throw new IllegalArgumentException(stat + " has no Floor");
        };
        return value(floor);
    }

    /** The durability Lunge Boring costs a block, or its default before the server's config has loaded. */
    public static int lungeBoringCost() {
        return value(LUNGE_BORING_COST);
    }

    /** The FE the netherite Bore Lance holds. */
    public static int netheriteLanceCapacity() {
        return value(NETHERITE_LANCE_CAPACITY);
    }

    /** The FE a netherite Bore Lance's lunge costs for each level of its Lunge. */
    public static int netheriteLanceLungeEnergy() {
        return value(NETHERITE_LANCE_LUNGE_ENERGY);
    }

    /** The FE a block mined in a netherite Bore Lance's Bore costs. */
    public static int netheriteLanceBoreEnergy() {
        return value(NETHERITE_LANCE_BORE_ENERGY);
    }

    /** The FE a block broken by a netherite Bore Lance's Lunge Boring costs. */
    public static int netheriteLanceLungeBoringEnergy() {
        return value(NETHERITE_LANCE_LUNGE_BORING_ENERGY);
    }

    /** The FE per tick a worn Suit gives a netherite Bore Lance in hand. */
    public static int netheriteLanceChargeRate() {
        return value(NETHERITE_LANCE_CHARGE_RATE);
    }

    /** The fraction of its capacity a Suit keeps when it charges a lance. */
    public static double netheriteLanceChargeReserve() {
        return value(NETHERITE_LANCE_CHARGE_RESERVE);
    }

    /** The server's value, or its default before the server's config has loaded, as on a client's tooltip. */
    private static int value(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** The server's value, or its default before the server's config has loaded, as on a client's tooltip. */
    private static double value(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    /** The least a boost upgrade's Cooldown falls toward, or its default before the server's config has loaded. */
    public static int boostCooldownFloor() {
        return SPEC.isLoaded() ? BOOST_COOLDOWN_FLOOR.get() : BOOST_COOLDOWN_FLOOR.getDefault();
    }

    /** The ticks each Boost thrusts for, or its default before the server's config has loaded. */
    public static int boostTicks() {
        return SPEC.isLoaded() ? BOOST_TICKS.get() : BOOST_TICKS.getDefault();
    }
}
