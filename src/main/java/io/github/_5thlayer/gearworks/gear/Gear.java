// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import java.util.List;

import io.github._5thlayer.gearworks.grid.GearGrid;
import io.github._5thlayer.gearworks.grid.Placement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * Gear as a stack: its Gear Grid, the FE it holds, and the totals of the Stats its upgrades give. A
 * suit has no FE of its own: what it can hold is the sum of its batteries.
 */
public final class Gear {

    private Gear() {
    }

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof GearItem;
    }

    /** The suit's Gear Grid, sized for its tier and holding the upgrades its saved layout places. */
    public static GearGrid<ItemStackTemplate> grid(ItemStack suit) {
        var size = GridSize.of(suit.typeHolder());
        return GearGrid.of(size.width(), size.height(), saved(suit));
    }

    /**
     * Changes the suit's layout, keeping no more FE than its batteries can now hold. The upgrades a
     * shrunk grid no longer fits are dropped from it, so the caller hands them back first
     * ({@link #takeOverflow}).
     */
    public static void setGrid(ItemStack suit, GearGrid<ItemStackTemplate> grid) {
        suit.set(GearContent.GRID.get(), new GridContents(grid.placements()));
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
        return (int) Math.min(Integer.MAX_VALUE, totals(suit).battery());
    }

    /** What the suit's upgrades give together, Stat by Stat. */
    public static Totals totals(ItemStack suit) {
        double battery = 0;
        double solar = 0;
        double speed = 0;
        int speedDraw = 0;
        for (var placement : grid(suit).placements()) {
            var upgrade = GearUpgrade.of(placement.item().typeHolder());
            if (upgrade == null) {
                // A pack took its entry away: it keeps its cells and gives nothing.
                continue;
            }
            switch (upgrade.stat()) {
                case BATTERY -> battery += upgrade.amount();
                case SOLAR -> solar += upgrade.amount();
                case SPEED -> {
                    speed += upgrade.amount();
                    speedDraw += upgrade.draw();
                }
            }
        }
        return new Totals(battery, solar, speed, speedDraw);
    }

    /**
     * The sums of a suit's Stats, before any Ceiling: battery in FE, solar in FE per tick, speed as a
     * fraction of base walking speed with the FE per tick it draws while walking.
     */
    public record Totals(double battery, double solar, double speed, int speedDraw) {

        /**
         * The walking speed added, under the Ceiling however many speed upgrades the suit holds (ADR
         * 0003): close to their sum while it is small, and never reaching the Ceiling.
         */
        public double speedBonus(double ceiling) {
            return ceiling <= 0 ? 0 : ceiling * (1 - Math.exp(-speed / ceiling));
        }
    }
}
