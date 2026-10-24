// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Solar makes its amount each tick in daylight under open sky, and nothing at night or under a roof. */
class SolarTest {

    @Test
    void makesItsAmountInDaylightUnderOpenSky() {
        assertEquals(50, Solar.output(50, true, true));
    }

    @Test
    void makesNothingAtNight() {
        assertEquals(0, Solar.output(50, false, true));
    }

    @Test
    void makesNothingUnderARoof() {
        assertEquals(0, Solar.output(50, true, false));
    }
}
