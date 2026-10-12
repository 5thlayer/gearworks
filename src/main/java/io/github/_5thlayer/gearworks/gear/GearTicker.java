// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import io.github._5thlayer.gearworks.Gearworks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The worn suit's Stats, each server tick: solar charges it in daylight under open sky, and speed
 * raises walking speed while it has the FE to draw, drawing it while the player walks.
 */
final class GearTicker {

    static final Identifier SPEED = Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "gear_speed");

    // A player standing still still reports a little movement from the client's rounding.
    private static final double WALKING = 1.0E-5;

    private GearTicker() {
    }

    static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (!player.level().isClientSide()) {
            tick(player);
        }
    }

    private static void tick(Player player) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var suit = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!Gear.is(suit)) {
            if (speed != null) {
                speed.removeModifier(SPEED);
            }
            return;
        }
        var totals = Gear.totals(suit);
        int energy = Gear.energy(suit);
        int before = energy;
        if (totals.solar() > 0 && inSunlight(player)) {
            energy = (int) Math.min(Gear.capacity(suit), energy + totals.solar());
        }
        boolean fast = totals.speed() > 0 && energy > 0 && energy >= totals.speedDraw();
        if (speed != null) {
            if (fast) {
                speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED,
                        totals.speedBonus(GearworksConfig.speedCeiling()), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else {
                speed.removeModifier(SPEED);
            }
        }
        if (fast && player.getKnownMovement().horizontalDistanceSqr() > WALKING) {
            energy -= totals.speedDraw();
        }
        if (energy != before) {
            Gear.setEnergy(suit, energy);
        }
    }

    private static boolean inSunlight(Player player) {
        var level = player.level();
        return level.isBrightOutside() && level.canSeeSky(BlockPos.containing(player.getEyePosition()));
    }
}
