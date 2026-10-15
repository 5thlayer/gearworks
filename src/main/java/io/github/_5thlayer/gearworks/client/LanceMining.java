// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import io.github._5thlayer.gearworks.lance.BoreLance;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jspecify.annotations.Nullable;

/**
 * Left-click mines with a Bore Lance. Vanilla's client jabs with any spear on every left-click and
 * never mines with one, so with the lance aimed at a block this starts mining in place of the jab, and
 * keeps mining while the button is held, as vanilla does for a pickaxe; aimed at air or a mob, it jabs.
 */
final class LanceMining {

    private LanceMining() {
    }

    static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        var minecraft = Minecraft.getInstance();
        if (!event.isAttack() || event.getHand() != InteractionHand.MAIN_HAND || !holdsLance(minecraft)) {
            return;
        }
        var block = aimedBlock(minecraft);
        if (block != null) {
            event.setCanceled(true);
            minecraft.gameMode.startDestroyBlock(block.getBlockPos(), block.getDirection());
        }
    }

    static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        if (!holdsLance(minecraft) || minecraft.player.isUsingItem()) {
            return;
        }
        boolean held = minecraft.screen == null && minecraft.options.keyAttack.isDown() && minecraft.mouseHandler.isMouseGrabbed();
        var block = held ? aimedBlock(minecraft) : null;
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
