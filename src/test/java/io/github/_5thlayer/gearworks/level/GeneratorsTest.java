// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The Generators fill the suit to its capacity, keeping a fraction of an FE for the next tick, and never
 * take FE from one holding more; kinetic makes its amount for each block moved, combustion burns a tick
 * of its fire for each upgrade, no more than the suit has room for, and metabolic makes no more than the
 * player's saturation, or the suit's room, gives.
 */
class GeneratorsTest {

    @Test
    void chargesByTheWholeFeMadeAndKeepsTheFraction() {
        assertEquals(new Generators.Charge(102, 0.5), Generators.charge(100, 1_000, 2.5));
    }

    @Test
    void fillsTheSuitNoFurtherThanItsCapacityAndKeepsNoFraction() {
        assertEquals(new Generators.Charge(1_000, 0), Generators.charge(980, 1_000, 40.5));
    }

    @Test
    void neverTakesFeFromASuitHoldingMoreThanItsCapacity() {
        assertEquals(new Generators.Charge(1_200, 0), Generators.charge(1_200, 1_000, 40));
    }

    @Test
    void kineticMakesItsAmountForEachBlockMoved() {
        assertEquals(25, Generators.kinetic(10, 2.5), 1e-9);
        assertEquals(0, Generators.kinetic(10, 0), 1e-9);
    }

    @Test
    void eachCombustionUpgradeBurnsATickOfTheFire() {
        assertEquals(new Generators.Burn(1_599, 2), Generators.burn(1_600, 1, 2, 1_000));
        assertEquals(new Generators.Burn(1_598, 5), Generators.burn(1_600, 2, 5, 1_000));
    }

    @Test
    void aFireShorterThanItsUpgradesGivesItsShare() {
        assertEquals(new Generators.Burn(0, 2), Generators.burn(1, 3, 6, 1_000));
    }

    @Test
    void aNearlyFullSuitBurnsOnlyTheTicksItHasRoomFor() {
        assertEquals(new Generators.Burn(1_599, 2), Generators.burn(1_600, 3, 6, 1.5));
    }

    @Test
    void noFireOrNoRoomBurnsNothing() {
        assertEquals(new Generators.Burn(0, 0), Generators.burn(0, 2, 5, 1_000));
        assertEquals(new Generators.Burn(1_600, 0), Generators.burn(1_600, 2, 5, 0));
    }

    @Test
    void metabolicMakesItsAmountWhileSaturationLasts() {
        assertEquals(20, Generators.metabolic(20, 5, 1_000, 1_000));
    }

    @Test
    void metabolicMakesNoMoreThanTheSaturationLeftGives() {
        assertEquals(10, Generators.metabolic(20, 0.01f, 1_000, 1_000));
        assertEquals(0, Generators.metabolic(20, 0, 1_000, 1_000));
    }

    @Test
    void metabolicMakesNoMoreThanTheSuitHasRoomFor() {
        assertEquals(7, Generators.metabolic(20, 5, 1_000, 7));
    }

    @Test
    void metabolicSpendsSaturationAtItsRateAndNeverUnderNone() {
        assertEquals(4.98f, Generators.saturationAfter(5, 20, 1_000), 1e-6);
        assertEquals(0, Generators.saturationAfter(0.001f, 20, 1_000));
    }
}
