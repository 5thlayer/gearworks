// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

/**
 * What solar charges a suit in one tick, free of Minecraft so it runs on a plain JVM: its amount in
 * daylight under open sky, up to the suit's capacity.
 */
public final class Solar {

    private Solar() {
    }

    /**
     * The FE in a suit holding {@code energy} of {@code capacity} after one tick of solar giving
     * {@code amount}, by day ({@code brightOutside}) or not, with or without {@code openSky} above it.
     */
    public static int charge(int energy, int capacity, double amount, boolean brightOutside, boolean openSky) {
        if (!brightOutside || !openSky) {
            return energy;
        }
        // Never down: an open grid may hold FE over what its batteries now take.
        return Math.max(energy, (int) Math.min(capacity, energy + amount));
    }
}
