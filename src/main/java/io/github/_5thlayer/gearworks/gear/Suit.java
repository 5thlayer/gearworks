// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import java.util.List;

import io.github._5thlayer.gearworks.grid.GearGrid;
import io.github._5thlayer.gearworks.grid.Placement;
import io.github._5thlayer.gearworks.level.Curves;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * The Suit as a stack: its Gear Grid, the FE it holds, and the totals of the Stats its upgrades give.
 * A suit has no FE of its own: what it can hold is the sum of its batteries.
 */
public final class Suit {

    private Suit() {
    }

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof SuitItem;
    }

    /** The suit's Gear Grid, sized for its tier and holding the upgrades its saved layout places. */
    public static GearGrid<ItemStackTemplate> grid(ItemStack suit) {
        var size = GridSize.of(suit.typeHolder());
        return GearGrid.of(size.width(), size.height(), saved(suit));
    }

    /**
     * Changes the suit's layout. Its FE stays as it was, so moving the only battery, which is picking it
     * up and placing it again, loses none: closing the grid keeps what the batteries then hold
     * ({@link #keepWhatBatteriesHold}). The upgrades a shrunk grid no longer fits are dropped from it,
     * so the caller hands them back first ({@link #takeOverflow}).
     */
    public static void setGrid(ItemStack suit, GearGrid<ItemStackTemplate> grid) {
        suit.set(GearContent.GRID.get(), new GridContents(grid.placements()));
    }

    /**
     * Whether the grid takes another upgrade of that kind wherever it fits: no, when its Stat is one a suit
     * holds once and the grid holds one already.
     */
    public static boolean admits(GearGrid<ItemStackTemplate> grid, GearUpgrade upgrade) {
        if (!upgrade.stat().onePerSuit()) {
            return true;
        }
        for (var placement : grid.placements()) {
            var placed = GearUpgrade.of(placement.item().typeHolder());
            if (placed != null && placed.stat() == upgrade.stat()) {
                return false;
            }
        }
        return true;
    }

    /** Keeps no more FE than the suit's batteries can hold, once its layout is settled. */
    public static void keepWhatBatteriesHold(ItemStack suit) {
        int capacity = capacity(suit);
        if (energy(suit) > capacity) {
            setEnergy(suit, capacity);
        }
    }

    /**
     * Takes out of the saved layout the upgrades its grid no longer fits, after a pack shrank the tier
     * or resized an upgrade, and gives them to the player, so no upgrade is lost.
     */
    public static void takeOverflow(Player player, ItemStack suit) {
        var size = GridSize.of(suit.typeHolder());
        List<Placement<ItemStackTemplate>> overflow = GearGrid.overflow(size.width(), size.height(), saved(suit));
        if (overflow.isEmpty()) {
            return;
        }
        setGrid(suit, grid(suit));
        for (var placement : overflow) {
            player.getInventory().placeItemBackInInventory(placement.item().create());
        }
    }

    private static List<Placement<ItemStackTemplate>> saved(ItemStack suit) {
        return suit.getOrDefault(GearContent.GRID.get(), GridContents.EMPTY).placements();
    }

    public static int energy(ItemStack suit) {
        return suit.getOrDefault(GearContent.ENERGY.get(), 0);
    }

    static void setEnergy(ItemStack suit, int energy) {
        suit.set(GearContent.ENERGY.get(), energy);
    }

    /** The FE the suit can hold: the sum of its batteries, as much as an int carries. */
    public static int capacity(ItemStack suit) {
        return (int) Math.min(Integer.MAX_VALUE, totals(suit).amount(Stat.BATTERY));
    }

    /** What the suit's upgrades give together, Stat by Stat. */
    public static Totals totals(ItemStack suit) {
        var totals = new Totals();
        for (var placement : grid(suit).placements()) {
            var upgrade = GearUpgrade.of(placement.item().typeHolder());
            if (upgrade == null) {
                // A pack took its entry away: it keeps its cells and gives nothing.
                continue;
            }
            if (upgrade.stat().onePerSuit() && totals.has(upgrade.stat())) {
                // One laid out before the grid refused a second: it gives and draws nothing.
                continue;
            }
            int level = GearUpgrade.level(placement.item());
            totals.add(upgrade.stat(), upgrade.amountAt(level), upgrade.drawAt(level));
        }
        return totals;
    }

    /**
     * The sums of a suit's Stats at their upgrades' Levels, before any Ceiling, each in its Stat's unit
     * ({@link Stat}), with the FE the Stat's upgrades draw together.
     */
    public static final class Totals {

        private final double[] amounts = new double[Stat.values().length];
        private final long[] draws = new long[Stat.values().length];
        private final int[] upgrades = new int[Stat.values().length];

        private Totals() {
        }

        private void add(Stat stat, double amount, int draw) {
            amounts[stat.ordinal()] += amount;
            draws[stat.ordinal()] += draw;
            upgrades[stat.ordinal()]++;
        }

        /** Whether the suit holds an upgrade of that Stat. */
        public boolean has(Stat stat) {
            return upgrades[stat.ordinal()] > 0;
        }

        /** The sum of what the suit's upgrades of that Stat give, before any Ceiling. */
        public double amount(Stat stat) {
            return amounts[stat.ordinal()];
        }

        /**
         * The FE the suit's upgrades of that Stat draw together, as much as an int carries: their sum, or
         * for a Stat whose upgrades share one price, their mean, rounded up.
         */
        public int draw(Stat stat) {
            long draw = draws[stat.ordinal()];
            int count = upgrades[stat.ordinal()];
            if (stat.draw() == Stat.Draw.SHARED && count > 0) {
                draw = (draw + count - 1) / count;
            }
            return (int) Math.min(Integer.MAX_VALUE, draw);
        }

        /** Whether the suit holds an upgrade of that Stat and the FE for its draw: an empty suit runs nothing. */
        public boolean canDraw(Stat stat, int energy) {
            return has(stat) && energy > 0 && energy >= draw(stat);
        }

        /**
         * What a Stat with a Ceiling gives, however many upgrades of it the suit holds (ADR 0003): close
         * to their sum while it is small, and never reaching the Ceiling.
         */
        public double underCeiling(Stat stat) {
            return Curves.underCeiling(amount(stat), GearworksConfig.ceiling(stat));
        }
    }
}
