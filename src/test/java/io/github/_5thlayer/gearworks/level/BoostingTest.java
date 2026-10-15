// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A Boost thrusts for its ticks and starts a Cooldown no shorter than them, so two never overlap; the
 * thrust pulls the velocity toward the strength along the gaze, as vanilla's rocket does a glider.
 */
class BoostingTest {

    @Test
    void aFreshPlayerIsReadyAndNotThrusting() {
        var boosting = new Boosting();

        assertTrue(boosting.ready());
        assertFalse(boosting.thrusting());
        assertEquals(0, boosting.cooldownLeft());
    }

    @Test
    void aBoostThrustsForItsTicksThenCoolsDownForTheRest() {
        var boosting = new Boosting();
        boosting.fire(1.5, 3, 5);

        for (int tick = 0; tick < 3; tick++) {
            assertTrue(boosting.thrusting(), "thrusting at tick " + tick);
            assertFalse(boosting.ready());
            boosting.tick();
        }
        assertFalse(boosting.thrusting());
        assertFalse(boosting.ready());
        boosting.tick();
        boosting.tick();
        assertTrue(boosting.ready());
    }

    @Test
    void theCooldownIsNeverShorterThanTheThrust() {
        var boosting = new Boosting();
        boosting.fire(1.5, 25, 10);

        assertEquals(25, boosting.cooldownLeft());
    }

    @Test
    void stoppingTheThrustKeepsTheCooldown() {
        var boosting = new Boosting();
        boosting.fire(1.5, 5, 10);

        boosting.stopThrust();

        assertFalse(boosting.thrusting());
        assertEquals(10, boosting.cooldownLeft());
    }

    @Test
    void theCooldownLeftIsAFractionOfTheWholeForTheHud() {
        var boosting = new Boosting();
        assertEquals(0, boosting.cooldownFraction());

        boosting.fire(1.5, 2, 4);
        assertEquals(1, boosting.cooldownFraction());
        boosting.tick();
        assertEquals(0.75, boosting.cooldownFraction());
    }

    @Test
    void theThrustIsVanillasRocketStep() {
        // FireworkRocketEntity: v + look × 0.1 + (look × 1.5 − v) × 0.5.
        assertEquals(0.85, Boosting.step(0, 1, 1.5), 1.0E-9);
        assertEquals(0.4 + 0.1 + (1.5 - 0.4) * 0.5, Boosting.step(0.4, 1, 1.5), 1.0E-9);
        assertEquals(0, Boosting.step(0, 0, 1.5));
    }

    @Test
    void aSustainedThrustSettlesJustOverItsStrength() {
        double velocity = 0;
        for (int tick = 0; tick < 25; tick++) {
            velocity = Boosting.step(velocity, 1, 1.5);
        }

        assertEquals(1.7, velocity, 1.0E-6);
    }

    @Test
    void underFlightsDragTheThrustSettlesWhereAGlidersDoesOnEveryAxis() {
        // Flight keeps 0.91 of the horizontal speed each tick and 0.6 of the vertical.
        for (double drag : new double[] {0.91, 0.6}) {
            double carried = 0;
            double moved = 0;
            for (int tick = 0; tick < 25; tick++) {
                moved = Boosting.step(carried, 1, 1.5, drag);
                carried = moved * drag;
            }

            assertEquals(1.7, moved, 1.0E-6, "under a drag of " + drag);
        }
    }

    @Test
    void aStrongerBoostGoesFaster() {
        assertTrue(Boosting.step(0, 1, 2.0) > Boosting.step(0, 1, 1.2));
    }
}
