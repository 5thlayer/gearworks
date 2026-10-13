// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * An upgrade's value grows linearly with its Level; a Stat with a Ceiling approaches it for the suit's
 * total, however many upgrades and Levels make that total up (ADR 0003).
 */
class CurvesTest {

    @Test
    void aLinearStatGrowsByTheSameStepEachLevel() {
        assertEquals(100_000, Curves.linear(100_000, 50_000, 0));
        assertEquals(150_000, Curves.linear(100_000, 50_000, 1));
        assertEquals(600_000, Curves.linear(100_000, 50_000, 10));
    }

    @Test
    void aStatWithNoStepStaysAtItsBase() {
        assertEquals(5, Curves.linear(5, 0, 1_000));
    }

    @Test
    void aDrawGrowsByItsStepAndSaturatesAtTheIntLimit() {
        assertEquals(20, Curves.draw(20, 5, 0));
        assertEquals(70, Curves.draw(20, 5, 10));
        assertEquals(Integer.MAX_VALUE, Curves.draw(20, 1_000_000, Integer.MAX_VALUE));
    }

    @Test
    void aSmallTotalUnderACeilingIsCloseToItsSum() {
        double bonus = Curves.underCeiling(0.05, 1.0);

        assertTrue(bonus < 0.05);
        assertTrue(bonus > 0.048);
    }

    @Test
    void theTotalNeverReachesTheCeiling() {
        assertTrue(Curves.underCeiling(1_000, 1.0) <= 1.0);
        assertTrue(Curves.underCeiling(3, 1.0) < 1.0);
    }

    @Test
    void twoUpgradesTogetherStayUnderTheOneCeiling() {
        double one = Curves.linear(0.3, 0.1, 20);
        double ceiling = 1.0;

        double alone = Curves.underCeiling(one, ceiling);
        double both = Curves.underCeiling(one + one, ceiling);

        assertTrue(both > alone, "a second upgrade adds speed");
        assertTrue(both < ceiling, "the two stay under the Ceiling");
        assertTrue(both < 2 * alone, "the Ceiling holds for the sum, not for each upgrade");
    }

    @Test
    void aZeroCeilingGivesNothing() {
        assertEquals(0, Curves.underCeiling(5, 0));
    }

    @Test
    void aDrawWithAFloorFallsByAboutItsStepAtFirst() {
        assertEquals(10, Curves.overFloor(10, 1, 1, 0));
        assertEquals(9, Curves.overFloor(10, 1, 1, 1));
    }

    @Test
    void aDrawWithAFloorNeverFallsUnderIt() {
        int previous = Curves.overFloor(10, 1, 1, 0);
        for (int level = 1; level <= 200; level++) {
            int draw = Curves.overFloor(10, 1, 1, level);
            assertTrue(draw <= previous, "Level " + level + " draws more than the last");
            assertTrue(draw >= 1, "Level " + level + " draws under the Floor");
            previous = draw;
        }
    }

    @Test
    void aLargeDrawClosesInOnItsFloorWithoutPassingIt() {
        // Flight's defaults: 200 FE/t falling by about 20 a Level toward a Floor of 20.
        assertEquals(181, Curves.overFloor(200, 20, 20, 1));
        int previous = 200;
        for (int level = 1; level <= 30; level++) {
            int draw = Curves.overFloor(200, 20, 20, level);
            assertTrue(draw < previous, "Level " + level + " lowers the draw no further");
            previous = draw;
        }
        for (int level = 30; level <= 10_000; level += 10) {
            assertTrue(Curves.overFloor(200, 20, 20, level) >= 20, "Level " + level + " draws under the Floor");
        }
        assertEquals(20, Curves.overFloor(200, 20, 20, 10_000), "the draw approaches the Floor");
    }

    @Test
    void aDrawAlreadyAtItsFloorStaysThere() {
        assertEquals(1, Curves.overFloor(1, 1, 2, 5));
    }
}
