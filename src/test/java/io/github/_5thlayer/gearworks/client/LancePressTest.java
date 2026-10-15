// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github._5thlayer.gearworks.client.LancePress.Action;
import org.junit.jupiter.api.Test;

/**
 * A press of attack with the Bore Lance aimed at a block jabs if it is let go within the hold time,
 * and mines if it is held past it, never both; mining goes on while it is held and stops when let go.
 * A jab at air held onto a block mines it, as vanilla's held left-click does.
 */
class LancePressTest {

    @Test
    void aPressLetGoBeforeTheHoldTimeJabs() {
        var press = new LancePress();
        press.press();
        assertEquals(Action.NONE, press.tick(true, true));
        assertEquals(Action.JAB, press.tick(false, true));
        assertEquals(Action.NONE, press.tick(false, true));
    }

    @Test
    void aPressHeldToTheHoldTimeMinesAndNeverJabs() {
        var press = new LancePress();
        press.press();
        for (int tick = 1; tick < LancePress.HOLD_TICKS; tick++) {
            assertEquals(Action.NONE, press.tick(true, true), "tick " + tick);
        }
        assertEquals(Action.MINE, press.tick(true, true));
        assertEquals(Action.MINE, press.tick(true, true));
        assertEquals(Action.STOP, press.tick(false, true));
        assertEquals(Action.NONE, press.tick(false, true));
    }

    @Test
    void nothingHappensWithoutAPress() {
        var press = new LancePress();
        assertEquals(Action.NONE, press.tick(true, true));
        assertEquals(Action.NONE, press.tick(false, true));
    }

    @Test
    void droppingThePressDoesNothingAndStopsAnyMining() {
        var waiting = new LancePress();
        waiting.press();
        assertEquals(Action.NONE, waiting.drop());
        assertEquals(Action.NONE, waiting.tick(false, true));

        var mining = new LancePress();
        mining.press();
        for (int tick = 0; tick < LancePress.HOLD_TICKS; tick++) {
            mining.tick(true, true);
        }
        assertEquals(Action.STOP, mining.drop());
        assertEquals(Action.NONE, mining.tick(true, true));
    }

    @Test
    void aJabAtAirHeldOntoABlockMinesIt() {
        var press = new LancePress();
        press.jabbed();
        assertEquals(Action.NONE, press.tick(true, false));
        assertEquals(Action.MINE, press.tick(true, true));
        assertEquals(Action.MINE, press.tick(true, false));
        assertEquals(Action.STOP, press.tick(false, false));
    }

    @Test
    void aJabAtAirLetGoMinesNothing() {
        var press = new LancePress();
        press.jabbed();
        assertEquals(Action.NONE, press.tick(false, false));
        assertEquals(Action.NONE, press.tick(true, true));
    }
}
