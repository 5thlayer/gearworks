// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.lance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The block of rock a Bore Lance breaks at once: laid on the face that was hit, centred on the hit
 * block and {@link #DEPTH} deep, in cells counted across the face, up it, and in from it. A tier's
 * widest Bore is its own; the key steps through it and the smaller ones.
 */
public enum Bore {

    // Narrowest first, so a Bore's ordinal orders the Bores by width.
    ONE(1, 1),
    THREE(3, 3),
    FIVE(5, 3);

    /** How many blocks deep every Bore goes, the hit block's layer included. */
    public static final int DEPTH = 3;

    private final int width;
    private final int height;
    private final List<Cell> cells;

    Bore(int width, int height) {
        this.width = width;
        this.height = height;
        this.cells = layOut(width, height);
    }

    /**
     * The Bore's cells, the hit block first and each layer before the one behind it, so a lance that
     * runs out partway leaves the deepest blocks standing.
     */
    public List<Cell> cells() {
        return cells;
    }

    /** This Bore, as a tier's widest, and the smaller ones: what the key steps through, widest first. */
    public List<Bore> andSmaller() {
        var bores = new ArrayList<Bore>();
        for (int i = ordinal(); i >= 0; i--) {
            bores.add(values()[i]);
        }
        return List.copyOf(bores);
    }

    /** The Bore after this one for a tier whose widest is that: the next smaller, or the widest again. */
    public Bore next(Bore widest) {
        var bores = widest.andSmaller();
        return bores.get((bores.indexOf(within(widest)) + 1) % bores.size());
    }

    /** This Bore, or the tier's widest where this is wider: one stored on a stack from a wider tier. */
    public Bore within(Bore widest) {
        return ordinal() > widest.ordinal() ? widest : this;
    }

    /** The Bore's name in a saved stack. */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The Bore of that saved name, or null for none. */
    public static @Nullable Bore bySerializedName(String name) {
        for (var bore : values()) {
            if (bore.serializedName().equals(name)) {
                return bore;
            }
        }
        return null;
    }

    /** Width by height by depth, as the tooltip and the action bar show it. */
    public String size() {
        return width + "×" + height + "×" + DEPTH;
    }

    private static List<Cell> layOut(int width, int height) {
        var cells = new ArrayList<Cell>();
        for (int in = 0; in < DEPTH; in++) {
            for (int up = -(height / 2); up <= height / 2; up++) {
                for (int across = -(width / 2); across <= width / 2; across++) {
                    cells.add(new Cell(across, up, in));
                }
            }
        }
        cells.sort(Comparator.comparingInt(Cell::in).thenComparingInt(cell -> Math.abs(cell.across()) + Math.abs(cell.up())));
        return List.copyOf(cells);
    }

    /** One block of a Bore, counted from the hit block across the face, up it, and in from it. */
    public record Cell(int across, int up, int in) {
    }
}
