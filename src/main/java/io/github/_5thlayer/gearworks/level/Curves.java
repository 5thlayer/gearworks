// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

/**
 * How a Stat grows with Levels, free of Minecraft so the curves run on a plain JVM. An upgrade's value
 * and its FE draw each grow by a step per Level; a Stat with a Ceiling bends the suit's total toward it
 * (ADR 0003), so stacking upgrades or Levels never reaches it.
 */
public final class Curves {

    private Curves() {
    }

    /** An upgrade's value at that Level: its base, and its step once per Level. */
    public static double linear(double base, double perLevel, int level) {
        return base + perLevel * level;
    }

    /** An upgrade's FE per tick at that Level, as much as an int carries. */
    public static int draw(int base, int perLevel, int level) {
        return (int) Math.min(Integer.MAX_VALUE, base + (long) perLevel * level);
    }

    /**
     * The value a Stat gives for the suit's total under its Ceiling: close to the total while it is
     * small, and never reaching the Ceiling.
     */
    public static double underCeiling(double total, double ceiling) {
        return ceiling <= 0 ? 0 : ceiling * (1 - Math.exp(-total / ceiling));
    }
}
