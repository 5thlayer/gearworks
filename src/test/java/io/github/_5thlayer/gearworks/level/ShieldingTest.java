// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * A shield takes its fraction of a hit and pays for each point it takes with FE, so it takes nothing
 * once the suit is empty.
 */
class ShieldingTest {

    @Test
    void aShieldWithFeAbsorbsItsFractionAndSpendsFeForEachPoint() {
        var hit = Shielding.absorb(10, 0.4, 10_000, 100);

        assertEquals(4, hit.absorbed(), 1e-6);
        assertEquals(400, hit.spent());
    }

    @Test
    void anEmptySuitAbsorbsNothing() {
        var hit = Shielding.absorb(10, 0.4, 0, 100);

        assertEquals(0, hit.absorbed());
        assertEquals(0, hit.spent());
    }

    @Test
    void aShieldLowOnFeAbsorbsOnlyWhatItsFePaysFor() {
        var hit = Shielding.absorb(10, 0.5, 250, 100);

        assertEquals(2.5, hit.absorbed(), 1e-6);
        assertEquals(250, hit.spent());
    }
}
