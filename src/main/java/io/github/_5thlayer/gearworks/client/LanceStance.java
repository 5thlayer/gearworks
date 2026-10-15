// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

/**
 * A Bore Lance held to mine stands in vanilla's Jab at full reach, rather than jabbing over and over.
 * Vanilla draws the Jab from how far through its swing the player is; the stance is that swing held at
 * {@link #reach}, where the Jab has reached out and not begun its pull-back. Each tick reads the swing
 * after vanilla has moved it on a tick.
 */
final class LanceStance {

    /** What a tick of mining does to the lance's swing. */
    enum Step {
        /** Starts a swing, which reaches out. */
        SWING,
        /** Leaves the swing to play on toward full reach. */
        WAIT,
        /** Sets the swing to {@link #held}: back toward full reach, a tick a tick, and there. */
        HOLD
    }

    private LanceStance() {
    }

    /** The tick of a swing of this many ticks where the Jab is at full reach: a fifth of the way through. */
    static int reach(int duration) {
        return (duration + 4) / 5;
    }

    /**
     * One tick of mining, the first when it starts, with the player's swing as it stands. Only mining
     * that starts from rest swings: a Jab already under way is let reach out, or wound back.
     */
    static Step step(boolean starting, boolean swinging, int swingTime, int duration) {
        if (!swinging) {
            return starting ? Step.SWING : Step.HOLD;
        }
        return swingTime < reach(duration) ? Step.WAIT : Step.HOLD;
    }

    /** The tick a held swing is set to, vanilla having just moved it on one: a tick back, never short of reach. */
    static int held(int swingTime, int duration) {
        return Math.max(reach(duration), swingTime - 2);
    }
}
