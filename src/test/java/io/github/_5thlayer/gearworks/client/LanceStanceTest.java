// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github._5thlayer.gearworks.client.LanceStance.Step;
import org.junit.jupiter.api.Test;

/**
 * A Bore Lance mining holds vanilla's Jab at full reach: one swing when mining starts from rest, none
 * when a Jab is already under way, which is let reach out or wound back to full reach and held there.
 * Each tick's swing is read after vanilla has moved it on a tick.
 */
class LanceStanceTest {

    /** The iron lance's swing, in ticks. */
    private static final int IRON_SWING = 19;

    @Test
    void miningFromRestSwingsOnceThenHoldsAtFullReach() {
        var reach = LanceStance.reach(IRON_SWING);
        assertEquals(Step.SWING, LanceStance.step(true, false, 0, IRON_SWING));
        for (int swingTime = 0; swingTime < reach; swingTime++) {
            assertEquals(Step.WAIT, LanceStance.step(false, true, swingTime, IRON_SWING), "swing tick " + swingTime);
        }
        assertEquals(Step.HOLD, LanceStance.step(false, true, reach, IRON_SWING));
        assertEquals(reach, LanceStance.held(reach, IRON_SWING));
        assertEquals(Step.HOLD, LanceStance.step(false, true, reach + 1, IRON_SWING));
        assertEquals(reach, LanceStance.held(reach + 1, IRON_SWING));
    }

    @Test
    void miningAfterAJabHasPassedFullReachWindsItBackWithoutASwing() {
        var reach = LanceStance.reach(IRON_SWING);
        assertEquals(Step.HOLD, LanceStance.step(true, true, reach + 4, IRON_SWING));
        // Vanilla moved it on to reach + 4 from reach + 3; it goes back to reach + 2, a tick a tick.
        assertEquals(reach + 2, LanceStance.held(reach + 4, IRON_SWING));
        assertEquals(reach + 1, LanceStance.held(reach + 3, IRON_SWING));
        assertEquals(reach, LanceStance.held(reach + 2, IRON_SWING));
    }

    @Test
    void miningWhileAJabStillReachesOutLetsItReachWithoutASwing() {
        var reach = LanceStance.reach(IRON_SWING);
        assertEquals(Step.WAIT, LanceStance.step(true, true, -1, IRON_SWING));
        assertEquals(Step.WAIT, LanceStance.step(true, true, reach - 1, IRON_SWING));
        assertEquals(Step.HOLD, LanceStance.step(true, true, reach, IRON_SWING));
    }

    /**
     * Vanilla's Jab, first person and third, has reached out by a fifth of the swing and starts its
     * pull-back at two fifths; full reach lies between, for every lance and whatever Haste or Mining
     * Fatigue makes of its swing.
     */
    @Test
    void fullReachLiesWhereVanillasJabHasReachedOutAndNotPulledBack() {
        for (int duration = 10; duration <= 40; duration++) {
            var progress = (float) LanceStance.reach(duration) / duration;
            assertTrue(progress >= 0.2f && progress < 0.4f, duration + " ticks: " + progress);
        }
    }
}
