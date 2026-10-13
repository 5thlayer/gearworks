// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Solar charges the suit by its amount each tick in daylight under open sky, and not at night or under
 * a roof; it fills the suit to its capacity and never takes FE from one holding more.
 */
class SolarTest {

    @Test
    void chargesByItsAmountInDaylightUnderOpenSky() {
        assertEquals(150, Solar.charge(100, 1_000, 50, true, true));
    }

    @Test
    void chargesNothingAtNight() {
        assertEquals(100, Solar.charge(100, 1_000, 50, false, true));
    }

    @Test
    void chargesNothingUnderARoof() {
        assertEquals(100, Solar.charge(100, 1_000, 50, true, false));
    }

    @Test
    void fillsTheSuitNoFurtherThanItsCapacity() {
        assertEquals(1_000, Solar.charge(980, 1_000, 50, true, true));
    }

    @Test
    void neverTakesFeFromASuitHoldingMoreThanItsCapacity() {
        assertEquals(1_200, Solar.charge(1_200, 1_000, 50, true, true));
    }
}
