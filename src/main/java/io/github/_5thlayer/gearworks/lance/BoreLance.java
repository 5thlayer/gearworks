// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.lance;

import io.github._5thlayer.gearworks.Gearworks;
import io.github._5thlayer.gearworks.gear.GearContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The Bore Lance as a stack: the Bore its key last chose, which it keeps, and whether it is worn out.
 * The blocks it mines and bores are in {@link #MINEABLE}, up to its tier's level.
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

    /** Whether the lance can break on: still in the player's main hand, not broken and not worn out. */
    public static boolean lasts(ItemStack lance, Player player) {
        return player.getMainHandItem() == lance && !lance.isEmpty() && !wornOut(lance);
    }

    /**
     * Whether the lance breaks that block in a Bore or a lunge: one of {@link #MINEABLE} it is not too low
     * a tier for, and not unbreakable.
     */
    public static boolean breaks(ItemStack lance, Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return !state.isAir() && state.getDestroySpeed(level, pos) >= 0 && lance.isCorrectToolForDrops(state);
    }

    private static BoreLanceItem tier(ItemStack lance) {
        return (BoreLanceItem) lance.getItem();
    }
}
