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
    SHIELD("shield"),
    /** Raises jump height while it has FE: the amount is the fraction of base jump strength added, and the draw the FE each jump spends. */
    JUMP("jump"),
    /**
     * Gives night vision while it draws FE: on or off, so a suit holds one, whose amount is not read. The
     * draw is FE per tick, and each Level lowers it toward the {@code nightVisionFloor}.
     */
    NIGHT_VISION("night_vision", true, true);

    public static final Codec<Stat> CODEC = StringRepresentable.fromEnum(Stat::values);

    private final String name;
    private final boolean onePerSuit;
    private final boolean drawFalls;

    Stat(String name) {
        this(name, false, false);
    }

    Stat(String name, boolean onePerSuit, boolean drawFalls) {
        this.name = name;
        this.onePerSuit = onePerSuit;
        this.drawFalls = drawFalls;
    }

    /** Whether a suit holds at most one upgrade of this Stat, which a second would add nothing to. */
    public boolean onePerSuit() {
        return onePerSuit;
    }

    /**
     * Whether its Levels lower the draw toward the Stat's Floor ({@link GearworksConfig#floor}), each
     * taking about {@code draw_per_level} off at first, rather than raise it.
     */
    public boolean drawFalls() {
        return drawFalls;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
