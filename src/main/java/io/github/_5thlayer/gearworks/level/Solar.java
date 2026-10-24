// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

/**
 * What solar makes in one tick, free of Minecraft so it runs on a plain JVM: its amount in daylight
 * under open sky, and nothing at night or under a roof.
 */
public final class Solar {

    private Solar() {
    }

    /** The FE solar giving {@code amount} makes in a tick, by day ({@code brightOutside}) or not, with or without {@code openSky}. */
    public static double output(double amount, boolean brightOutside, boolean openSky) {
        return brightOutside && openSky ? amount : 0;
    }
}
