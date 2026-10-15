// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.lance;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.gearworks.gear.GearworksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Bore Lance mining a block breaks its Bore with it: the blocks around and behind the hit one, on
 * the face that was hit, each as the player breaking it, so each passes the same protection, drops
 * where it stood under the lance's Fortune or Silk Touch, and costs the lance its durability, or on FE
 * {@link GearworksConfig#lanceBoreEnergy()}, the hit block's included. A Bore breaks only what the lance
 * mines at its tier and nothing harder than the hit block, and none at all while sneaking, from a block
 * the lance doesn't mine, or from a worn-out or dry lance, which mine the hit block alone and free.
 */
public final class BoreBreaker {

    // Each of a Bore's blocks is mined with the lance too; those break alone.
    private static boolean breaking;

    private BoreBreaker() {
    }

    /**
     * After the lance mined a block: a block of a Bore pays its FE, and any other the player is mining
     * breaks the rest of its Bore.
     */
    static void mined(ItemStack lance, ServerPlayer player, BlockPos pos, BlockState hit) {
        if (breaking) {
            BoreLance.spend(lance, GearworksConfig.lanceBoreEnergy());
        } else {
            bore(lance, player, pos, hit);
        }
    }

    /**
     * Breaks the rest of the Bore around a block the player is mining with the lance, once the lance has
     * paid for that block and before it is removed, so a lance that wears out partway never leaves the
     * hit block undropped.
     */
    private static void bore(ItemStack lance, ServerPlayer player, BlockPos pos, BlockState hit) {
        int price = GearworksConfig.lanceBoreEnergy();
        if (player.isShiftKeyDown() || !lance.isCorrectToolForDrops(hit) || !BoreLance.lasts(lance, player, price)) {
            return;
        }
        // Durability paid for the hit block as it was mined; FE pays for it as the Bore's first block.
        BoreLance.spend(lance, price);
        var level = player.level();
        float hardness = hit.getDestroySpeed(level, pos);
        var blocks = blocks(BoreLance.bore(lance), pos, face(player, pos, hit), player.getDirection());
        breaking = true;
        try {
            // The first is the hit block, which the player is breaking already.
            for (var block : blocks.subList(1, blocks.size())) {
                if (!BoreLance.lasts(lance, player, price)) {
                    break;
                }
                if (bores(lance, level, block, hardness)) {
                    player.gameMode.destroyBlock(block);
                }
            }
        } finally {
            breaking = false;
        }
    }

    /** Whether a Bore takes that block: one the lance {@linkplain BoreLance#breaks breaks}, no harder than the hit block. */
    private static boolean bores(ItemStack lance, Level level, BlockPos pos, float hardness) {
        return BoreLance.breaks(lance, level, pos) && level.getBlockState(pos).getDestroySpeed(level, pos) <= hardness;
    }

    /**
     * Where the Bore's cells lie for a block hit on that face by a player facing that way: in from the
     * face, and across and up it. A wall's up is the sky's; a floor's or a ceiling's is the way the player
     * faces, so a wide Bore lies across their path.
     */
    public static List<BlockPos> blocks(Bore bore, BlockPos hit, Direction face, Direction facing) {
        var up = face.getAxis().isVertical() ? facing : Direction.UP;
        var across = face.getAxis().isVertical() ? facing.getClockWise() : face.getClockWise();
        var in = face.getOpposite();
        var blocks = new ArrayList<BlockPos>();
        for (var cell : bore.cells()) {
            blocks.add(hit.relative(across, cell.across()).relative(up, cell.up()).relative(in, cell.in()));
        }
        return blocks;
    }

    /**
     * The face of the block the player is looking at, which the server isn't told when mining ends; if
     * the look misses its shape, the face turned most toward the player.
     */
    private static Direction face(Player player, BlockPos pos, BlockState state) {
        var eye = player.getEyePosition();
        var look = player.getViewVector(1);
        var hit = state.getShape(player.level(), pos).clip(eye, eye.add(look.scale(player.blockInteractionRange() + 1)), pos);
        return hit != null ? hit.getDirection() : Direction.getApproximateNearest(look).getOpposite();
    }
}
