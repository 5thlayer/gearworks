// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import io.github._5thlayer.gearworks.lance.BoreLance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
 * Mining holds the lance at full reach ({@link LanceStance}) until let go, when it pulls back. Alt-tabbing,
 * a screen, or the lance leaving the hand ends a press without a jab, and the stance at once.
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
            if (PRESS.drop() == LancePress.Action.DROP && minecraft.gameMode != null) {
                minecraft.gameMode.stopDestroyBlock();
                if (minecraft.player != null) {
                    dropStance(minecraft.player);
                }
            }
            return;
        }
        var action = PRESS.tick(minecraft.options.keyAttack.isDown(), aimedBlock(minecraft) != null);
        switch (action) {
            case NONE -> {
            }
            case JAB -> jab(minecraft);
            case START, MINE -> {
                stance(minecraft.player, action == LancePress.Action.START);
                mine(minecraft);
            }
            // Let go, the swing plays on from full reach: vanilla's pull-back.
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
        }
    }

    /**
     * Holds the lance in its {@link LanceStance} for a tick of mining. The one swing reaches the server,
     * so others see a single Jab; the held swing stays on this client.
     */
    private static void stance(LocalPlayer player, boolean starting) {
        var duration = player.getCurrentSwingDuration();
        switch (LanceStance.step(starting, player.swinging, player.swingTime, duration)) {
            case SWING -> player.swing(InteractionHand.MAIN_HAND);
            case WAIT -> {
            }
            case HOLD -> {
                // Both ends of the frame's easing, or vanilla reads a swing wound back as one wrapping round.
                var tick = LanceStance.held(player.swingTime, duration);
                player.swinging = true;
                player.swingTime = tick;
                player.attackAnim = (float) tick / duration;
                player.oAttackAnim = player.attackAnim;
            }
        }
    }

    /** Ends the stance at once, without vanilla's pull-back. */
    private static void dropStance(LocalPlayer player) {
        player.swinging = false;
        player.swingTime = 0;
        player.attackAnim = 0;
        player.oAttackAnim = 0;
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
