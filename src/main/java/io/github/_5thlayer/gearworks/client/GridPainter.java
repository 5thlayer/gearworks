// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import io.github._5thlayer.gearworks.grid.GearGrid;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * Draws a container's panel and slots in vanilla's colours, and a Gear Grid with the upgrades placed
 * over it, for the Gear Grid's screen and the Gear Bench's.
 */
final class GridPainter {

    static final int PANEL = 0xFFC6C6C6;
    static final int EDGE = 0xFF000000;
    static final int LIGHT = 0xFFFFFFFF;
    static final int SHADE = 0xFF555555;
    static final int SLOT = 0xFF8B8B8B;
    static final int SLOT_SHADE = 0xFF373737;
    static final int LABEL = 0xFF404040;
    private static final int UPGRADE = 0xFF6F7F8F;
    private static final int UPGRADE_EDGE = 0xFF2F3F4F;

    private GridPainter() {
    }

    /** A container's raised panel. */
    static void panel(GuiGraphicsExtractor graphics, int left, int top, int width, int height) {
        graphics.fill(left, top, left + width, top + height, EDGE);
        graphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, LIGHT);
        graphics.fill(left + 3, top + 3, left + width - 1, top + height - 1, SHADE);
        graphics.fill(left + 3, top + 3, left + width - 3, top + height - 3, PANEL);
    }

    /** A sunken slot whose inside is that rectangle. */
    static void sunk(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, SLOT_SHADE);
        graphics.fill(x, y, x + w + 1, y + h + 1, LIGHT);
        graphics.fill(x, y, x + w, y + h, SLOT);
    }

    /**
     * The grid's cells, {@code cell} pixels apart from its top-left at {@code left, top}, and the
     * upgrades over them; an upgrade's item shows where its rectangle has room for it.
     */
    static void grid(GuiGraphicsExtractor graphics, GearGrid<ItemStackTemplate> grid, int left, int top, int cell) {
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                sunk(graphics, left + x * cell, top + y * cell, cell - 2, cell - 2);
            }
        }
        for (var placement : grid.placements()) {
            int x0 = left + placement.x() * cell;
            int y0 = top + placement.y() * cell;
            int w = placement.width() * cell - 2;
            int h = placement.height() * cell - 2;
            graphics.fill(x0, y0, x0 + w, y0 + h, UPGRADE_EDGE);
            graphics.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + h - 1, UPGRADE);
            if (w >= 16 && h >= 16) {
                graphics.item(placement.item().create(), x0 + (w - 16) / 2, y0 + (h - 16) / 2);
            }
        }
    }
}
