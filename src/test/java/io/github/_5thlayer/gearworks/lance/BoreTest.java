// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.lance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * A Bore lies on the face that was hit, three deep, and a tier steps through its own Bore and the
 * smaller ones, widest first.
 */
class BoreTest {

    @Test
    void aOneByOneBoreIsTheHitBlockAndTheTwoBehindIt() {
        assertEquals(List.of(new Bore.Cell(0, 0, 0), new Bore.Cell(0, 0, 1), new Bore.Cell(0, 0, 2)), Bore.ONE.cells());
    }

    @Test
    void aThreeByThreeBoreIsTwentySevenBlocksAroundTheHitOne() {
        assertCells(Bore.THREE, 1, 1);
    }

    @Test
    void aFiveByThreeBoreIsFiveAcrossAndThreeUp() {
        assertCells(Bore.FIVE, 2, 1);
    }

    @Test
    void aBoreBreaksTheHitBlockFirstAndEachLayerBeforeTheOneBehindIt() {
        for (var bore : Bore.values()) {
            var cells = bore.cells();
            assertEquals(new Bore.Cell(0, 0, 0), cells.getFirst(), bore + " starts elsewhere");
            for (int i = 1; i < cells.size(); i++) {
                assertTrue(cells.get(i - 1).in() <= cells.get(i).in(), bore + " goes back a layer at " + cells.get(i));
            }
        }
    }

    @Test
    void aTierStepsThroughItsOwnBoreAndTheSmallerOnesWidestFirst() {
        assertEquals(List.of(Bore.THREE, Bore.ONE), Bore.THREE.andSmaller());
        assertEquals(List.of(Bore.FIVE, Bore.THREE, Bore.ONE), Bore.FIVE.andSmaller());
        assertEquals(List.of(Bore.ONE), Bore.ONE.andSmaller());
    }

    @Test
    void theNextBoreIsTheNextSmallerOneThenTheTiersWidestAgain() {
        assertEquals(Bore.ONE, Bore.THREE.next(Bore.THREE));
        assertEquals(Bore.THREE, Bore.ONE.next(Bore.THREE));
        assertEquals(Bore.THREE, Bore.FIVE.next(Bore.FIVE));
        assertEquals(Bore.FIVE, Bore.ONE.next(Bore.FIVE));
    }

    @Test
    void aTierWithOneBoreStaysOnIt() {
        assertEquals(Bore.ONE, Bore.ONE.next(Bore.ONE));
    }

    @Test
    void aBoreWiderThanTheTiersIsTakenDownToTheTiersWidest() {
        assertEquals(Bore.ONE, Bore.FIVE.within(Bore.ONE));
        assertEquals(Bore.THREE, Bore.THREE.within(Bore.FIVE));
    }

    @Test
    void aBoreIsNamedByItsSize() {
        assertEquals("1×1×3", Bore.ONE.size());
        assertEquals("3×3×3", Bore.THREE.size());
        assertEquals("5×3×3", Bore.FIVE.size());
    }

    private static void assertCells(Bore bore, int across, int up) {
        var cells = bore.cells();
        assertEquals((2 * across + 1) * (2 * up + 1) * Bore.DEPTH, cells.size());
        assertEquals(cells.size(), new HashSet<>(cells).size(), "a cell is listed twice");
        for (var cell : cells) {
            assertTrue(Math.abs(cell.across()) <= across && Math.abs(cell.up()) <= up && cell.in() >= 0 && cell.in() < Bore.DEPTH,
                    cell + " lies outside " + bore);
        }
    }
}
