// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

import java.util.List;

/**
 * What raising an upgrade from one Level to the next costs, free of Minecraft so the formula runs on a
 * plain JVM; the material {@code M} is whatever the caller counts, a tag in the game. Each Level costs
 * {@code base × growth^level}, rounded up: the FE from Level 0, and the materials of the stage the Level
 * falls in, counted from that stage's first Level, so a stage's first Level costs its base.
 *
 * @param energy the FE the first Level costs
 * @param growth how much more each Level costs than the last, at least 1
 * @param stages the materials from each stage's first Level on, the first from Level 0, in order
 */
public record CostCurve<M>(long energy, double growth, List<Stage<M>> stages) {

    public CostCurve {
        stages = List.copyOf(stages);
        if (energy < 0 || !(growth >= 1)) {
            throw new IllegalArgumentException("a cost grows from a non-negative base: " + energy + " × " + growth);
        }
        if (stages.isEmpty() || stages.getFirst().from() != 0) {
            throw new IllegalArgumentException("the first stage starts at Level 0");
        }
        for (int i = 1; i < stages.size(); i++) {
            if (stages.get(i).from() <= stages.get(i - 1).from()) {
                throw new IllegalArgumentException("the stages start at rising Levels: " + stages);
            }
        }
    }

    /** The cost of raising an upgrade at that Level to the next. */
    public Cost<M> at(int level) {
        var stage = stages.getFirst();
        for (var next : stages) {
            if (next.from() <= level) {
                stage = next;
            }
        }
        int steps = level - stage.from();
        var materials = stage.materials().stream()
                .map(material -> new Material<>(material.what(), (int) Math.min(Integer.MAX_VALUE, scaled(material.count(), steps))))
                .toList();
        return new Cost<>(scaled(energy, level), materials);
    }

    /** {@code base × growth^steps}, rounded up and as much as a long carries. */
    private long scaled(long base, int steps) {
        double value = base * Math.pow(growth, steps);
        // A product that lands a hair over a whole number is that number.
        return value >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.ceil(value - 1.0E-9);
    }

    /** The materials from that Level on, until the next stage's. */
    public record Stage<M>(int from, List<Material<M>> materials) {

        public Stage {
            materials = List.copyOf(materials);
        }
    }

    public record Material<M>(M what, int count) {
    }

    /** One Level's cost: its FE and its materials. */
    public record Cost<M>(long energy, List<Material<M>> materials) {

        public Cost {
            materials = List.copyOf(materials);
        }
    }
}
