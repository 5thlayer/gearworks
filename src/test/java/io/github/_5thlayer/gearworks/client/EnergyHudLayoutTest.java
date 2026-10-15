// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import static io.github._5thlayer.gearworks.client.EnergyHudLayout.BAR_HEIGHT;
import static io.github._5thlayer.gearworks.client.EnergyHudLayout.BAR_WIDTH;
import static io.github._5thlayer.gearworks.client.EnergyHudLayout.COOLDOWN_HEIGHT;
import static io.github._5thlayer.gearworks.client.EnergyHudLayout.FILL_WIDTH;
import static io.github._5thlayer.gearworks.client.EnergyHudLayout.GAP;
import static io.github._5thlayer.gearworks.client.EnergyHudLayout.MARGIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github._5thlayer.gearworks.client.EnergyHudLayout.Corner;
import org.junit.jupiter.api.Test;

/**
 * The energy reading hugs its corner: aligned to the corner's side, with the text at the screen's
 * edge, the bar inside it, and a Boost's Cooldown past the bar.
 */
class EnergyHudLayoutTest {

    private static final int WIDTH = 480;
    private static final int HEIGHT = 270;
    private static final int TEXT = 100;
    private static final int LINE = 9;

    @Test
    void topLeftPutsTheTextAboveTheBarAgainstTheLeftEdge() {
        var layout = EnergyHudLayout.place(Corner.TOP_LEFT, WIDTH, HEIGHT, TEXT, LINE);

        assertEquals(new EnergyHudLayout(MARGIN, MARGIN, MARGIN, MARGIN + LINE + GAP), layout);
    }

    @Test
    void topRightAlignsBothToTheRightEdge() {
        var layout = EnergyHudLayout.place(Corner.TOP_RIGHT, WIDTH, HEIGHT, TEXT, LINE);

        assertEquals(new EnergyHudLayout(WIDTH - MARGIN - TEXT, MARGIN, WIDTH - MARGIN - BAR_WIDTH, MARGIN + LINE + GAP), layout);
    }

    @Test
    void bottomLeftPutsTheBarAboveTheText() {
        var layout = EnergyHudLayout.place(Corner.BOTTOM_LEFT, WIDTH, HEIGHT, TEXT, LINE);

        int textY = HEIGHT - MARGIN - LINE;
        assertEquals(new EnergyHudLayout(MARGIN, textY, MARGIN, textY - GAP - BAR_HEIGHT), layout);
    }

    @Test
    void bottomRightHugsTheBottomRightCorner() {
        var layout = EnergyHudLayout.place(Corner.BOTTOM_RIGHT, WIDTH, HEIGHT, TEXT, LINE);

        int textY = HEIGHT - MARGIN - LINE;
        assertEquals(new EnergyHudLayout(WIDTH - MARGIN - TEXT, textY, WIDTH - MARGIN - BAR_WIDTH, textY - GAP - BAR_HEIGHT), layout);
    }

    @Test
    void theBarFillsInProportionToTheStoredEnergy() {
        assertEquals(0, EnergyHudLayout.filled(0, 400_000));
        assertEquals(FILL_WIDTH / 2, EnergyHudLayout.filled(200_000, 400_000));
        assertEquals(FILL_WIDTH, EnergyHudLayout.filled(400_000, 400_000));
    }

    @Test
    void anyEnergyShowsAtLeastOnePixelAndOnlyFullFillsTheBar() {
        assertEquals(1, EnergyHudLayout.filled(1, 400_000));
        assertEquals(FILL_WIDTH - 1, EnergyHudLayout.filled(399_999, 400_000));
    }

    @Test
    void theFillStaysInsideTheBar() {
        assertEquals(FILL_WIDTH, EnergyHudLayout.filled(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(FILL_WIDTH, EnergyHudLayout.filled(500, 400));
    }

    @Test
    void theCooldownSitsUnderTheBarInATopCorner() {
        var layout = EnergyHudLayout.place(Corner.TOP_LEFT, WIDTH, HEIGHT, TEXT, LINE);

        assertEquals(layout.barY() + BAR_HEIGHT + GAP, layout.cooldownY(Corner.TOP_LEFT));
    }

    @Test
    void theCooldownSitsOverTheBarInABottomCorner() {
        var layout = EnergyHudLayout.place(Corner.BOTTOM_RIGHT, WIDTH, HEIGHT, TEXT, LINE);

        assertEquals(layout.barY() - GAP - COOLDOWN_HEIGHT, layout.cooldownY(Corner.BOTTOM_RIGHT));
    }

    @Test
    void theCooldownEmptiesAsItRunsAndShowsUntilItEnds() {
        assertEquals(FILL_WIDTH, EnergyHudLayout.cooldownFilled(1));
        assertEquals(FILL_WIDTH / 2, EnergyHudLayout.cooldownFilled(0.5));
        assertEquals(1, EnergyHudLayout.cooldownFilled(0.001));
        assertEquals(0, EnergyHudLayout.cooldownFilled(0));
    }
}
