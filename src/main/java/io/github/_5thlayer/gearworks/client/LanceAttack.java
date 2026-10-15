// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import io.github._5thlayer.gearworks.lance.BoreLance;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jspecify.annotations.Nullable;

/**
 * Left-click jabs with a Bore Lance and holding it mines. Vanilla's client jabs with any spear on every
 * left-click and never mines with one, so a press with the lance aimed at a block waits to see whether it
 * is a click, which then jabs, or a hold, which mines for as long as it is held, as vanilla does for a
 * pickaxe ({@link LancePress}). Aimed at air or a mob, a press jabs at once, and held onto a block mines.
 * Alt-tabbing, a screen, or the lance leaving the hand ends a press without a jab.
 */
final class LanceAttack {

    private static final LancePress PRESS = new LancePress();

    private LanceAttack() {
    }

    static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        var minecraft = Minecraft.getInstance();
        if (!event.isAttack() || event.getHand() != InteractionHand.MAIN_HAND || !holdsLance(minecraft)) {
            return;
        }
        if (aimedBlock(minecraft) == null) {
            // Vanilla jabs.
            PRESS.jabbed();
            return;
        }
        // Neither jab nor swing yet: the ticks that follow tell which it is.
        event.setCanceled(true);
        event.setSwingHand(false);
        PRESS.press();
    }

    static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (!holdsLance(minecraft) || minecraft.screen != null || !minecraft.mouseHandler.isMouseGrabbed()
                || minecraft.player.isUsingItem()) {
            if (PRESS.drop() == LancePress.Action.STOP && minecraft.gameMode != null) {
                minecraft.gameMode.stopDestroyBlock();
            }
            return;
        }
        switch (PRESS.tick(minecraft.options.keyAttack.isDown(), aimedBlock(minecraft) != null)) {
            case NONE -> {
            }
            case JAB -> jab(minecraft);
            case MINE -> mine(minecraft);
            case STOP -> minecraft.gameMode.stopDestroyBlock();
        }
    }

    /** Vanilla's jab, as its left-click makes it, held back until the press proved a click. */
    private static void jab(Minecraft minecraft) {
        var player = minecraft.player;
        var lance = player.getMainHandItem();
        var weapon = lance.get(DataComponents.PIERCING_WEAPON);
        if (weapon != null && !player.isHandsBusy() && !player.isSpectator() && !player.cannotAttackWithItem(lance, 0)) {
            minecraft.gameMode.piercingAttack(weapon);
            minecraft.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /** Mines the block aimed at, starting on it when it is a new one, as vanilla's held left-click does. */
    private static void mine(Minecraft minecraft) {
        var block = aimedBlock(minecraft);
        if (block == null) {
            minecraft.gameMode.stopDestroyBlock();
        } else if (minecraft.gameMode.continueDestroyBlock(block.getBlockPos(), block.getDirection())) {
            minecraft.level.addBreakingBlockEffect(block.getBlockPos(), block.getDirection(), block);
            minecraft.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static boolean holdsLance(Minecraft minecraft) {
        return minecraft.player != null && minecraft.gameMode != null && BoreLance.is(minecraft.player.getMainHandItem());
    }

    private static @Nullable BlockHitResult aimedBlock(Minecraft minecraft) {
        if (minecraft.hitResult instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK
                && !minecraft.level.getBlockState(block.getBlockPos()).isAir()) {
            return block;
        }
        return null;
    }
}
