// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Gear Grid: a two-dimensional area whose Gear Upgrades each cover a rectangle of it, inside its
 * edge and over no other. Immutable, and free of Minecraft so the rules run on a plain JVM; the
 * upgrade it holds is whatever {@code T} the caller places.
 */
public final class GearGrid<T> {

    private final int width;
    private final int height;
    private final List<Placement<T>> placements;

    private GearGrid(int width, int height, List<Placement<T>> placements) {
        this.width = width;
        this.height = height;
        this.placements = List.copyOf(placements);
    }

    public static <T> GearGrid<T> empty(int width, int height) {
        return new GearGrid<>(width, height, List.of());
    }

    /**
     * The grid of that size holding a saved layout. A placement that no longer fits, out of a grid a
     * pack has since shrunk or over one placed before it, is left out: {@link #overflow} names those.
     */
    public static <T> GearGrid<T> of(int width, int height, List<Placement<T>> saved) {
        GearGrid<T> grid = empty(width, height);
        for (Placement<T> placement : saved) {
            grid = grid.place(placement).orElse(grid);
        }
        return grid;
    }

    /** The placements of a saved layout that {@link #of} leaves out of a grid of that size. */
    public static <T> List<Placement<T>> overflow(int width, int height, List<Placement<T>> saved) {
        var kept = of(width, height, saved).placements;
        var left = new ArrayList<>(saved);
        kept.forEach(left::remove);
        return List.copyOf(left);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public List<Placement<T>> placements() {
        return placements;
    }

    /** The upgrade covering that cell, if any. */
    public Optional<Placement<T>> at(int x, int y) {
        return placements.stream().filter(placement -> placement.covers(x, y)).findFirst();
    }

    /** Whether a rectangle with that top-left cell and size lies inside the edge and over no upgrade. */
    public boolean fits(int x, int y, int itemWidth, int itemHeight) {
        return new Placement<>(x, y, itemWidth, itemHeight, null).fitsIn(width, height)
                && placements.stream().noneMatch(placement -> placement.overlaps(x, y, itemWidth, itemHeight));
    }

    /** The grid with the upgrade placed at that top-left cell, or nothing where it does not fit. */
    public Optional<GearGrid<T>> place(int x, int y, int itemWidth, int itemHeight, T item) {
        return place(new Placement<>(x, y, itemWidth, itemHeight, item));
    }

    private Optional<GearGrid<T>> place(Placement<T> placement) {
        if (!fits(placement.x(), placement.y(), placement.width(), placement.height())) {
            return Optional.empty();
        }
        var next = new ArrayList<>(placements);
        next.add(placement);
        return Optional.of(new GearGrid<>(width, height, next));
    }

    /** The grid with the upgrade placed in the topmost, then leftmost, rectangle it fits. */
    public Optional<GearGrid<T>> placeFirstFit(int itemWidth, int itemHeight, T item) {
        for (int y = 0; y + itemHeight <= height; y++) {
            for (int x = 0; x + itemWidth <= width; x++) {
                var placed = place(x, y, itemWidth, itemHeight, item);
                if (placed.isPresent()) {
                    return placed;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * The grid with that placement's upgrade swapped for another in the same rectangle and the same
     * place in the layout, as raising its Level does; unchanged if the grid does not hold it.
     */
    public GearGrid<T> replace(Placement<T> placement, T item) {
        var next = new ArrayList<>(placements);
        int index = next.indexOf(placement);
        if (index < 0) {
            return this;
        }
        next.set(index, new Placement<>(placement.x(), placement.y(), placement.width(), placement.height(), item));
        return new GearGrid<>(width, height, next);
    }

    /** The grid without that placement. */
    public GearGrid<T> remove(Placement<T> placement) {
        var next = new ArrayList<>(placements);
        next.remove(placement);
        return new GearGrid<>(width, height, next);
    }
}
