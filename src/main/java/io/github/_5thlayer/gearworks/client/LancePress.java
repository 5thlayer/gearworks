// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

/**
 * Whether a press of attack with the Bore Lance aimed at a block is a click or a hold. Let go within
 * {@link #HOLD_TICKS} it jabs, and its Lunge with it; held that long it mines, and never jabs, so mining
 * doesn't lunge the player into the rock at every block. A press at air jabs at once, and held on
 * until it reaches a block, mines it, as vanilla's held left-click does.
 */
final class LancePress {

    /** How long a press is held before it mines rather than jabs: a fifth of a second. */
    static final int HOLD_TICKS = 4;

    private enum State { IDLE, PRESSED, JABBED, MINING }

    /** What a tick of the press, or its drop, does. */
    enum Action {
        /** Nothing yet, or nothing more. */
        NONE,
        /** The press was a click: it jabs. */
        JAB,
        /** The press is a hold, and mining starts. */
        START,
        /** Mining goes on. */
        MINE,
        /** Let go: mining stops. */
        STOP,
        /** The press ended without being let go: mining stops at once. */
        DROP
    }

    private State state = State.IDLE;
    private int heldTicks;

    /** Attack was pressed with the lance aimed at a block; the next ticks tell a click from a hold. */
    void press() {
        state = State.PRESSED;
        heldTicks = 0;
    }

    /** Attack was pressed with the lance aimed at air or a mob, and jabbed at once. */
    void jabbed() {
        state = State.JABBED;
    }

    /** One client tick, with attack still held down or let go, and the lance aimed at a block or not. */
    Action tick(boolean down, boolean onBlock) {
        return switch (state) {
            case IDLE -> Action.NONE;
            case PRESSED -> {
                if (!down) {
                    state = State.IDLE;
                    yield Action.JAB;
                }
                if (++heldTicks < HOLD_TICKS) {
                    yield Action.NONE;
                }
                state = State.MINING;
                yield Action.START;
            }
            case JABBED -> {
                if (!down) {
                    state = State.IDLE;
                    yield Action.NONE;
                }
                if (!onBlock) {
                    yield Action.NONE;
                }
                state = State.MINING;
                yield Action.START;
            }
            case MINING -> {
                if (down) {
                    yield Action.MINE;
                }
                state = State.IDLE;
                yield Action.STOP;
            }
        };
    }

    /** The press ends without a jab, as when the lance leaves the hand or a screen opens; mining stops. */
    Action drop() {
        var action = state == State.MINING ? Action.DROP : Action.NONE;
        state = State.IDLE;
        return action;
    }
}
