// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import static io.github._5thlayer.gearworks.client.GlowPulse.DIM;
import static io.github._5thlayer.gearworks.client.GlowPulse.PERIOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The worn suit's gem glows brightest once each period and dims to a floor halfway between, never
 * going dark.
 */
class GlowPulseTest {

    @Test
    void peaksAtTheStartOfEachPeriod() {
        assertEquals(1, GlowPulse.brightness(0), 1e-6);
        assertEquals(1, GlowPulse.brightness(PERIOD), 1e-6);
        assertEquals(1, GlowPulse.brightness(3 * PERIOD), 1e-6);
    }

    @Test
    void dimsToItsFloorHalfwayThrough() {
        assertEquals(DIM, GlowPulse.brightness(PERIOD / 2), 1e-6);
    }

    @Test
    void staysBetweenItsFloorAndFullBrightness() {
        for (float age = 0; age < 2 * PERIOD; age += 0.25F) {
            float brightness = GlowPulse.brightness(age);
            assertTrue(brightness >= DIM - 1e-6 && brightness <= 1 + 1e-6, age + " gives " + brightness);
        }
        assertTrue(DIM > 0, "the gem goes dark");
    }

    @Test
    void fadesItsGlowWithoutTintingIt() {
        assertEquals(0xFFFFFFFF, GlowPulse.color(0));
        assertEquals(Math.round(DIM * 255) << 24 | 0xFFFFFF, GlowPulse.color(PERIOD / 2));
    }
}
