// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

/**
 * How bright the worn suit's gem glows at the age of the entity wearing it, so each wearer pulses in
 * its own phase: brightest once every {@link #PERIOD} ticks, easing down to {@link #DIM} halfway
 * between. The suits' animated icons pulse at the same period.
 */
final class GlowPulse {

    /** Ticks from one peak to the next. */
    static final float PERIOD = 40;

    /** The brightness at the pulse's lowest, a fraction of full. */
    static final float DIM = 0.35F;

    private GlowPulse() {
    }

    static float brightness(float ageInTicks) {
        float wave = (float) Math.cos(2 * Math.PI * ageInTicks / PERIOD);
        return DIM + (1 - DIM) * (wave + 1) / 2;
    }

    /**
     * The white that fades the glow to that brightness: the glow is blended over the gem, not added
     * to it, so it dims by showing less of itself and never paints the gem darker than it is.
     */
    static int color(float ageInTicks) {
        return Math.round(brightness(ageInTicks) * 255) << 24 | 0xFFFFFF;
    }
}
