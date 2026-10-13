// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The effects Gearworks gives Gear. Gearworks owns every Stat; a Gear Upgrade's data map entry only
 * names one and its amount, which each Stat reads in its own unit.
 */
public enum Stat implements StringRepresentable {
    /** Stores FE: the amount is the FE it holds. */
    BATTERY("battery"),
    /** Adds FE in daylight under open sky: the amount is FE per tick. */
    SOLAR("solar"),
    /** Raises walking speed while it draws FE: the amount is the fraction of base speed added. */
    SPEED("speed"),
    /**
     * Absorbs damage while the suit has FE: the amount is the fraction of each hit absorbed, and the draw
     * the FE spent for each point of damage absorbed. Several shields absorb more together, each point at
     * their draws' mean, so stacking them buys absorption without raising its price.
     */
    SHIELD("shield", Draw.SHARED, PerSuit.MANY),
    /** Raises jump height while it has FE: the amount is the fraction of base jump strength added, and the draw the FE each jump spends. */
    JUMP("jump"),
    /**
     * Gives night vision while it draws FE: on or off, so a suit holds one, whose amount is not read. The
     * draw is FE per tick, and each Level lowers it toward the {@code nightVisionFloor}.
     */
    NIGHT_VISION("night_vision", Draw.FALLS, PerSuit.ONE);

    public static final Codec<Stat> CODEC = StringRepresentable.fromEnum(Stat::values);

    private final String name;
    private final Draw draw;
    private final PerSuit perSuit;

    Stat(String name) {
        this(name, Draw.SUMMED, PerSuit.MANY);
    }

    Stat(String name, Draw draw, PerSuit perSuit) {
        this.name = name;
        this.draw = draw;
        this.perSuit = perSuit;
    }

    /** How the Stat's upgrades draw FE together, and which way their Levels move it. */
    public Draw draw() {
        return draw;
    }

    /** Whether a suit holds at most one upgrade of this Stat, which a second would add nothing to. */
    public boolean onePerSuit() {
        return perSuit == PerSuit.ONE;
    }

    /** How a Stat's upgrades draw FE together, and which way a Level moves an upgrade's draw. */
    public enum Draw {
        /** Each upgrade draws its own, and a Level raises it by {@code draw_per_level}. */
        SUMMED,
        /** The upgrades share one price, their draws' mean, and a Level raises an upgrade's. */
        SHARED,
        /**
         * A Level lowers the draw toward the Stat's Floor ({@link GearworksConfig#floor}), taking about
         * {@code draw_per_level} off at first.
         */
        FALLS
    }

    /** How many upgrades of a Stat a suit holds. */
    public enum PerSuit {
        MANY,
        ONE
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
