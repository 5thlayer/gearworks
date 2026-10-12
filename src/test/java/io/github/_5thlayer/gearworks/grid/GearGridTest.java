// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * A Gear Upgrade covers a rectangle of the Gear Grid, inside its edge and over no other upgrade.
 */
class GearGridTest {

    private static final GearGrid<String> EMPTY = GearGrid.empty(5, 4);

    @Test
    void anUpgradeFitsInsideTheGrid() {
        var grid = EMPTY.place(3, 2, 2, 2, "speed").orElseThrow();

        assertEquals(List.of(new Placement<>(3, 2, 2, 2, "speed")), grid.placements());
    }

    @Test
    void anUpgradeCoversEveryCellOfItsRectangle() {
        var grid = EMPTY.place(1, 1, 2, 3, "battery").orElseThrow();

        assertEquals("battery", grid.at(2, 3).orElseThrow().item());
        assertEquals(Optional.empty(), grid.at(3, 1));
        assertEquals(Optional.empty(), grid.at(1, 0));
    }

    @Test
    void anUpgradeCannotCrossTheEdge() {
        assertEquals(Optional.empty(), EMPTY.place(4, 0, 2, 1, "speed"));
        assertEquals(Optional.empty(), EMPTY.place(0, 3, 1, 2, "battery"));
        assertEquals(Optional.empty(), EMPTY.place(-1, 0, 1, 1, "solar"));
        assertEquals(Optional.empty(), EMPTY.place(0, -1, 1, 1, "solar"));
    }

    @Test
    void anUpgradeLargerThanTheGridNeverFits() {
        assertEquals(Optional.empty(), EMPTY.place(0, 0, 6, 1, "wide"));
    }

    @Test
    void anUpgradeCannotOverlapAnother() {
        var grid = EMPTY.place(1, 1, 2, 2, "speed").orElseThrow();

        assertEquals(Optional.empty(), grid.place(2, 2, 1, 1, "solar"));
        assertEquals(Optional.empty(), grid.place(0, 0, 2, 2, "speed"));
    }

    @Test
    void upgradesFitSideBySide() {
        var grid = EMPTY.place(1, 1, 2, 2, "speed").orElseThrow()
                .place(3, 1, 1, 2, "battery").orElseThrow()
                .place(1, 0, 2, 1, "solar").orElseThrow();

        assertEquals(3, grid.placements().size());
    }

    @Test
    void removingAnUpgradeFreesItsCells() {
        var grid = EMPTY.place(1, 1, 2, 2, "speed").orElseThrow();
        var removed = grid.remove(grid.at(2, 2).orElseThrow());

        assertTrue(removed.placements().isEmpty());
        assertTrue(removed.place(2, 2, 1, 1, "solar").isPresent());
    }

    @Test
    void movingAnUpgradeIsRemovingAndPlacingIt() {
        var grid = EMPTY.place(0, 0, 2, 2, "speed").orElseThrow();
        var picked = grid.at(1, 1).orElseThrow();
        var moved = grid.remove(picked).place(1, 1, 2, 2, picked.item()).orElseThrow();

        assertEquals(List.of(new Placement<>(1, 1, 2, 2, "speed")), moved.placements());
    }

    @Test
    void replacingAnUpgradeKeepsItsRectangleAndItsPlaceInTheLayout() {
        var grid = EMPTY.place(0, 0, 2, 2, "speed").orElseThrow().place(3, 0, 1, 2, "battery").orElseThrow();

        var replaced = grid.replace(grid.at(1, 1).orElseThrow(), "faster speed");

        assertEquals(List.of(new Placement<>(0, 0, 2, 2, "faster speed"), new Placement<>(3, 0, 1, 2, "battery")),
                replaced.placements());
    }

    @Test
    void replacingAnUpgradeTheGridDoesNotHoldChangesNothing() {
        var grid = EMPTY.place(0, 0, 2, 2, "speed").orElseThrow();

        assertEquals(grid.placements(), grid.replace(new Placement<>(3, 3, 1, 1, "solar"), "battery").placements());
    }

    @Test
    void theFirstFitIsTheTopmostThenLeftmostFreeRectangle() {
        var grid = EMPTY.place(0, 0, 2, 1, "solar").orElseThrow();

        assertEquals(new Placement<>(2, 0, 2, 2, "speed"), grid.placeFirstFit(2, 2, "speed").orElseThrow().placements().get(1));
    }

    @Test
    void aFullGridHasNoFirstFit() {
        var grid = EMPTY.place(0, 0, 5, 4, "everything").orElseThrow();

        assertEquals(Optional.empty(), grid.placeFirstFit(1, 1, "solar"));
    }

    @Test
    void aLayoutLargerThanItsGridKeepsWhatFits() {
        // A pack that shrinks the tier's grid keeps a saved layout's upgrades that still fit.
        var saved = List.of(new Placement<>(0, 0, 1, 1, "kept"), new Placement<>(4, 3, 1, 1, "dropped"));

        var grid = GearGrid.of(3, 3, saved);

        assertEquals(List.of(new Placement<>(0, 0, 1, 1, "kept")), grid.placements());
        assertEquals(List.of(new Placement<>(4, 3, 1, 1, "dropped")), GearGrid.overflow(3, 3, saved));
    }
}
