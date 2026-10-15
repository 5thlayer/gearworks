// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * A worn Suit charges a netherite Bore Lance by up to its rate each tick, only from the FE it holds above
 * its reserve, and no further than the lance holds.
 */
class LanceChargeTest {

    private static final int RATE = 1_000;
    private static final double RESERVE = 0.25;

    @Test
    void movesTheRateFromASuitWellAboveItsReserve() {
        assertEquals(1_000, LanceCharge.transfer(80_000, 100_000, 0, 1_000_000, RATE, RESERVE));
    }

    @Test
    void movesNothingFromASuitAtItsReserve() {
        assertEquals(0, LanceCharge.transfer(25_000, 100_000, 0, 1_000_000, RATE, RESERVE));
    }

    @Test
    void movesNothingFromASuitBelowItsReserve() {
        assertEquals(0, LanceCharge.transfer(10_000, 100_000, 0, 1_000_000, RATE, RESERVE));
    }

    @Test
    void neverTakesTheSuitBelowItsReserve() {
        assertEquals(400, LanceCharge.transfer(25_400, 100_000, 0, 1_000_000, RATE, RESERVE));
    }

    @Test
    void fillsTheLanceNoFurtherThanItHolds() {
        assertEquals(300, LanceCharge.transfer(80_000, 100_000, 999_700, 1_000_000, RATE, RESERVE));
    }

    @Test
    void keepsItsReserveRoundedUp() {
        assertEquals(25_000, LanceCharge.kept(100_000, RESERVE));
        assertEquals(1, LanceCharge.kept(3, RESERVE));
    }

    @Test
    void aSuitWithNoBatteryChargesNothing() {
        assertEquals(0, LanceCharge.transfer(0, 0, 0, 1_000_000, RATE, RESERVE));
    }
}
