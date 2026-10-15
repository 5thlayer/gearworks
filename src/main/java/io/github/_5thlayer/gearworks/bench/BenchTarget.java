// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.bench;

import java.util.Optional;

import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearUpgrade;
import io.github._5thlayer.gearworks.gear.LevelCost;
import io.github._5thlayer.gearworks.grid.Placement;
import io.github._5thlayer.gearworks.level.CostCurve.Cost;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * The upgrade a Gear Bench is set to raise, in the suit it holds, with its Level and what the next
 * one costs: null when no {@code gearworks:level_cost} entry names it, so it is never raised. The
 * server and the screen both read it, so the screen shows the cost the server charges.
 */
public record BenchTarget(Placement<ItemStackTemplate> placement, int level, @Nullable Cost<TagKey<Item>> cost) {

    /** The upgrade covering that cell of the suit's grid, if the bench holds a suit and one covers it. */
    public static Optional<BenchTarget> of(ItemStack suit, int x, int y) {
        if (!Suit.is(suit) || x < 0 || y < 0) {
            return Optional.empty();
        }
        return Suit.grid(suit).at(x, y).map(placement -> {
            var item = placement.item();
            int level = GearUpgrade.level(item);
            var cost = LevelCost.of(item.typeHolder());
            return new BenchTarget(placement, level, cost == null ? null : cost.at(level));
        });
    }

    /** Whether the stock and FE cover the next Level's cost: each material in its own slot. */
    public boolean stocked(ResourceHandler<ItemResource> stock, long energy) {
        if (cost == null || energy < cost.energy()) {
            return false;
        }
        for (int i = 0; i < cost.materials().size(); i++) {
            if (stocked(stock, i) < cost.materials().get(i).count()) {
                return false;
            }
        }
        return true;
    }

    /** How much of the material at that index the stock holds; what is not that material counts for none. */
    public long stocked(ResourceHandler<ItemResource> stock, int index) {
        if (cost == null || index >= cost.materials().size() || index >= stock.size()) {
            return 0;
        }
        var resource = stock.getResource(index);
        return !resource.isEmpty() && resource.typeHolder().is(cost.materials().get(index).what()) ? stock.getAmountAsLong(index) : 0;
    }

    /** The upgrade one Level up, everything else about it kept. */
    public ItemStackTemplate raised() {
        var stack = placement.item().create();
        stack.set(GearContent.LEVEL.get(), level + 1);
        return ItemStackTemplate.fromNonEmptyStack(stack);
    }
}
