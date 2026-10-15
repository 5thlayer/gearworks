// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.lance;

import java.util.Optional;

import io.github._5thlayer.gearworks.Gearworks;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Bore Lance as a stack: the Bore its key last chose, which it keeps, whether it is worn out, and on
 * the tier that runs on FE, the FE it holds. The blocks it mines and bores are in {@link #MINEABLE}, up
 * to its tier's level.
 */
public final class BoreLance {

    /** The blocks a Bore Lance mines at its speed and breaks in a Bore: by default, a pickaxe's. */
    public static final TagKey<Block> MINEABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "mineable/bore_lance"));

    private BoreLance() {
    }

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof BoreLanceItem;
    }

    /** The Bore the lance breaks: the one its key chose, else its tier's widest. */
    public static Bore bore(ItemStack lance) {
        var widest = tier(lance).widest();
        return lance.getOrDefault(GearContent.BORE.get(), widest).within(widest);
    }

    /** Steps the lance on to its next Bore, and gives back the one it now breaks. */
    public static Bore cycle(ItemStack lance) {
        var next = bore(lance).next(tier(lance).widest());
        lance.set(GearContent.BORE.get(), next);
        return next;
    }

    /** Whether the lance is a tier that never breaks, stopped at its last durability: no Bore and no Lunge. */
    public static boolean wornOut(ItemStack lance) {
        return tier(lance).wornOut(lance);
    }

    /** Whether the lance is a tier that runs on FE rather than durability. */
    public static boolean runsOnEnergy(ItemStack lance) {
        return is(lance) && tier(lance).upkeep() == BoreLanceItem.Upkeep.ENERGY;
    }

    /** The FE the lance holds: none on a tier that doesn't run on it. */
    public static int energy(ItemStack lance) {
        return runsOnEnergy(lance) ? lance.getOrDefault(GearContent.ENERGY.get(), 0) : 0;
    }

    /** Puts FE into a lance that runs on it, up to what it holds, and gives back how much went in. */
    public static int fill(ItemStack lance, int fe) {
        if (!runsOnEnergy(lance)) {
            return 0;
        }
        int energy = energy(lance);
        int added = Math.max(0, Math.min(fe, GearworksConfig.lanceCapacity() - energy));
        if (added > 0) {
            lance.set(GearContent.ENERGY.get(), energy + added);
        }
        return added;
    }

    /** Takes that FE from a lance that runs on it, down to none; one on durability pays nothing here. */
    static void spend(ItemStack lance, int fe) {
        if (runsOnEnergy(lance) && fe > 0) {
            lance.set(GearContent.ENERGY.get(), Math.max(0, energy(lance) - fe));
        }
    }

    /** Whether the lance runs on FE and lacks a Bore's block of it: it mines single blocks and neither bores nor lunges. */
    public static boolean dry(ItemStack lance) {
        return runsOnEnergy(lance) && energy(lance) < Math.max(1, GearworksConfig.lanceBoreEnergy());
    }

    /**
     * Whether the lance can break on, a block at a time at that price in FE: still in the player's main
     * hand, not broken, not worn out, and on FE, holding the price.
     */
    public static boolean lasts(ItemStack lance, Player player, int fe) {
        if (player.getMainHandItem() != lance || lance.isEmpty()) {
            return false;
        }
        return runsOnEnergy(lance) ? energy(lance) >= fe : !wornOut(lance);
    }

    /** Pays for a lunge that fired: on FE, its price a level. */
    static void payForLunge(ItemStack lance) {
        spend(lance, tier(lance).lungeEnergy());
    }

    /** Whether the lance runs on FE and holds its lunge's price: such a lunge has no food gate. */
    static boolean affordsLunge(ItemStack lance) {
        return runsOnEnergy(lance) && energy(lance) >= tier(lance).lungeEnergy();
    }

    /**
     * Whether the lance mines that block as a Bore's: one of {@link #MINEABLE} the lance is not too low a
     * tier for, and not unbreakable.
     */
    public static boolean breaks(ItemStack lance, Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return !state.isAir() && state.getDestroySpeed(level, pos) >= 0 && lance.isCorrectToolForDrops(state);
    }

    /**
     * Whether the block needs a higher tier than the lance's to mine: one its tool denies drops, as
     * obsidian is to iron. What else the block is, rock or not, doesn't matter.
     */
    public static boolean beyondTier(ItemStack lance, BlockState state) {
        var tool = lance.get(DataComponents.TOOL);
        if (tool == null) {
            return false;
        }
        for (var rule : tool.rules()) {
            if (rule.correctForDrops().equals(Optional.of(false)) && state.is(rule.blocks())) {
                return true;
            }
        }
        return false;
    }

    private static BoreLanceItem tier(ItemStack lance) {
        return (BoreLanceItem) lance.getItem();
    }
}
