// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.grid;

/**
 * A Gear Upgrade placed in a Gear Grid: the rectangle it covers, from its top-left cell, and the
 * upgrade itself. Its size is the one its data map entry gave when it was placed, so a pack that
 * resizes an upgrade later never makes a saved layout overlap.
 */
public record Placement<T>(int x, int y, int width, int height, T item) {

    public Placement {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("a placement covers at least one cell: " + width + "x" + height);
        }
    }

    boolean covers(int cellX, int cellY) {
        return cellX >= x && cellX < x + width && cellY >= y && cellY < y + height;
    }

    boolean overlaps(int otherX, int otherY, int otherWidth, int otherHeight) {
        return otherX < x + width && x < otherX + otherWidth && otherY < y + height && y < otherY + otherHeight;
    }

    boolean fitsIn(int gridWidth, int gridHeight) {
        return x >= 0 && y >= 0 && x + width <= gridWidth && y + height <= gridHeight;
    }
}
