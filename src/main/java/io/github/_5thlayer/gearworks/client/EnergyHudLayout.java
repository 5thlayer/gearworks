// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

/**
 * Where the HUD's energy reading sits in its corner: aligned to the corner's side, the text against
 * the screen's edge and the bar inside it. It moves aside for nothing: the corner is the player's to
 * choose.
 *
 * @param textX the text's left edge
 * @param textY the text's top edge
 * @param barX  the bar's left edge, its frame included
 * @param barY  the bar's top edge, its frame included
 */
record EnergyHudLayout(int textX, int textY, int barX, int barY) {

    static final int MARGIN = 2;
    static final int GAP = 1;
    static final int BAR_WIDTH = 64;
    static final int BAR_HEIGHT = 5;
    /** The bar inside its one-pixel frame, which the stored FE fills. */
    static final int FILL_WIDTH = BAR_WIDTH - 2;

    enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

        boolean right() {
            return this == TOP_RIGHT || this == BOTTOM_RIGHT;
        }

        boolean bottom() {
            return this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
        }
    }

    static EnergyHudLayout place(Corner corner, int screenWidth, int screenHeight, int textWidth, int lineHeight) {
        int textX = corner.right() ? screenWidth - MARGIN - textWidth : MARGIN;
        int barX = corner.right() ? screenWidth - MARGIN - BAR_WIDTH : MARGIN;
        if (corner.bottom()) {
            int textY = screenHeight - MARGIN - lineHeight;
            return new EnergyHudLayout(textX, textY, barX, textY - GAP - BAR_HEIGHT);
        }
        return new EnergyHudLayout(textX, MARGIN, barX, MARGIN + lineHeight + GAP);
    }

    /**
     * How much of {@link #FILL_WIDTH} the stored FE fills: some as soon as the suit holds any, and all
     * of it only when the suit is full.
     */
    static int filled(int energy, int capacity) {
        if (energy <= 0 || capacity <= 0) {
            return 0;
        }
        if (energy >= capacity) {
            return FILL_WIDTH;
        }
        long width = (long) energy * FILL_WIDTH / capacity;
        return (int) Math.clamp(width, 1, FILL_WIDTH - 1);
    }
}
