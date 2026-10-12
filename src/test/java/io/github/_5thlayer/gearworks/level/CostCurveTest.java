// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import io.github._5thlayer.gearworks.level.CostCurve.Material;
import io.github._5thlayer.gearworks.level.CostCurve.Stage;
import org.junit.jupiter.api.Test;

/**
 * A Level costs {@code base × growth^level} in FE, and in the materials of the stage it falls in,
 * counted from the stage's first Level.
 */
class CostCurveTest {

    private static final CostCurve<String> CURVE = new CostCurve<>(1_000, 2.0, List.of(
            new Stage<>(0, List.of(new Material<>("iron", 4), new Material<>("redstone", 3))),
            new Stage<>(10, List.of(new Material<>("netherite", 1), new Material<>("nether_star", 1)))));

    @Test
    void theFirstLevelCostsTheBase() {
        assertEquals(new CostCurve.Cost<>(1_000, List.of(new Material<>("iron", 4), new Material<>("redstone", 3))), CURVE.at(0));
    }

    @Test
    void eachLevelCostsTheLastTimesTheGrowth() {
        var cost = CURVE.at(3);

        assertEquals(8_000, cost.energy());
        assertEquals(List.of(new Material<>("iron", 32), new Material<>("redstone", 24)), cost.materials());
    }

    @Test
    void aStagesMaterialsCountFromItsFirstLevel() {
        var cost = CURVE.at(11);

        assertEquals(1_000L << 11, cost.energy());
        assertEquals(List.of(new Material<>("netherite", 2), new Material<>("nether_star", 2)), cost.materials());
    }

    @Test
    void aFractionalCountRoundsUp() {
        var curve = new CostCurve<>(10, 1.5, List.of(new Stage<>(0, List.of(new Material<>("iron", 1)))));

        assertEquals(List.of(new Material<>("iron", 2)), curve.at(1).materials());
        assertEquals(15, curve.at(1).energy());
        assertEquals(List.of(new Material<>("iron", 3)), curve.at(2).materials());
        assertEquals(23, curve.at(2).energy());
    }

    @Test
    void aHighLevelSaturatesRatherThanOverflowing() {
        var cost = CURVE.at(1_000);

        assertEquals(Long.MAX_VALUE, cost.energy());
        assertEquals(Integer.MAX_VALUE, cost.materials().getFirst().count());
    }

    @Test
    void theStagesStartAtLevelZeroInOrder() {
        assertThrows(IllegalArgumentException.class, () -> new CostCurve<>(1, 2.0, List.of(new Stage<>(1, List.of()))));
        assertThrows(IllegalArgumentException.class, () -> new CostCurve<>(1, 2.0,
                List.of(new Stage<>(0, List.of()), new Stage<>(5, List.of()), new Stage<>(5, List.of()))));
    }
}
