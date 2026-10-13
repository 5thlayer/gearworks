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
     * the FE spent for each point of damage absorbed.
     */
    SHIELD("shield"),
    /** Raises jump height while it has FE: the amount is the fraction of base jump strength added, and the draw the FE each jump spends. */
    JUMP("jump"),
    /** Gives night vision while it draws FE: on with any upgrade, whose amount is not read; the draw is FE per tick. */
    NIGHT_VISION("night_vision");

    public static final Codec<Stat> CODEC = StringRepresentable.fromEnum(Stat::values);

    private final String name;

    Stat(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
