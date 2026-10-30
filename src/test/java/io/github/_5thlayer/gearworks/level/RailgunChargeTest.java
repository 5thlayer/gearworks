// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * A Railgun's Charge advances a tick at a time while it is held, and on its last tick makes it Charged
 * and pays its cost, or waits at full until it holds the cost; Quick Charge shortens it and Power raises
 * the shot's damage as a bow's.
 */
class RailgunChargeTest {

    private static final int COST = 1_000;
    private static final int TICKS = 25;

    @Test
    void advancesATickWhileHeld() {
        assertEquals(new RailgunCharge.Step(4, false, 0), RailgunCharge.tick(3, true, false, 50_000, COST, TICKS, 0));
    }

    @Test
    void keepsItsProgressWhilePutAway() {
        assertEquals(new RailgunCharge.Step(12, false, 0), RailgunCharge.tick(12, false, false, 50_000, COST, TICKS, 0));
    }

    @Test
    void becomesChargedOnItsLastTickPayingExactlyTheCost() {
        assertEquals(new RailgunCharge.Step(TICKS, true, COST), RailgunCharge.tick(TICKS - 1, true, false, 50_000, COST, TICKS, 0));
    }

    @Test
    void takesTheChargeTimeFromNothingToCharged() {
        var step = new RailgunCharge.Step(0, false, 0);
        int ticks = 0;
        while (!step.charged()) {
            step = RailgunCharge.tick(step.progress(), true, false, 50_000, COST, TICKS, 0);
            ticks++;
        }
        assertEquals(TICKS, ticks);
    }

    @Test
    void chargesOnTheCostExactly() {
        assertEquals(new RailgunCharge.Step(TICKS, true, COST), RailgunCharge.tick(TICKS - 1, true, false, COST, COST, TICKS, 0));
    }

    @Test
    void waitsAtFullWithoutTheCost() {
        assertEquals(new RailgunCharge.Step(TICKS, false, 0), RailgunCharge.tick(TICKS - 1, true, false, COST - 1, COST, TICKS, 0));
        assertEquals(new RailgunCharge.Step(TICKS, false, 0), RailgunCharge.tick(TICKS, true, false, COST - 1, COST, TICKS, 0));
    }

    @Test
    void chargesOnceTheCostArrives() {
        assertEquals(new RailgunCharge.Step(TICKS, true, COST), RailgunCharge.tick(TICKS, true, false, COST, COST, TICKS, 0));
    }

    @Test
    void staysChargedUntilItFires() {
        assertEquals(new RailgunCharge.Step(TICKS, true, 0), RailgunCharge.tick(TICKS, true, true, 50_000, COST, TICKS, 0));
        assertEquals(new RailgunCharge.Step(TICKS, true, 0), RailgunCharge.tick(TICKS, false, true, 0, COST, TICKS, 0));
    }

    @Test
    void doesNothingPutAwayAtFull() {
        assertEquals(new RailgunCharge.Step(TICKS, false, 0), RailgunCharge.tick(TICKS, false, false, 50_000, COST, TICKS, 0));
    }

    @Test
    void chargesAtOnceWhenQuickChargeCutsItBelowItsProgress() {
        assertEquals(new RailgunCharge.Step(10, true, COST), RailgunCharge.tick(20, true, false, 50_000, COST, TICKS, 3));
    }

    @ParameterizedTest
    @CsvSource({"0, 25", "1, 20", "2, 15", "3, 10", "4, 10", "5, 10"})
    void quickChargeTakesFiveTicksALevelDownToTen(int quickCharge, int ticks) {
        assertEquals(ticks, RailgunCharge.ticks(TICKS, quickCharge));
    }

    @Test
    void neverLengthensAChargeConfiguredUnderTheFloor() {
        assertEquals(4, RailgunCharge.ticks(4, 0));
        assertEquals(4, RailgunCharge.ticks(4, 3));
        assertEquals(10, RailgunCharge.ticks(12, 1));
    }

    @Test
    void aFreeChargeCostsNothing() {
        assertEquals(new RailgunCharge.Step(TICKS, true, 0), RailgunCharge.tick(TICKS - 1, true, false, 0, 0, TICKS, 0));
    }

    @ParameterizedTest
    @CsvSource({"0, 16", "1, 24", "2, 28", "3, 32", "4, 36", "5, 40"})
    void powerRaisesTheDamageAsOnABow(int power, double damage) {
        assertEquals(damage, RailgunCharge.damage(16, power), 1e-9);
    }
}
