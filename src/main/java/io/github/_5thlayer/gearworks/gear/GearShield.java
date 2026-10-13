// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import io.github._5thlayer.gearworks.level.Shielding;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * The worn suit's shield: it absorbs its fraction of each hit that armor has not stopped, under the
 * shield Ceiling however many shield upgrades the suit holds (ADR 0003), and spends FE for each point
 * it absorbs, so an empty suit absorbs nothing. What ignores invulnerability, such as the void or
 * {@code /kill}, ignores the shield too.
 */
final class GearShield {

    private GearShield() {
    }

    static void onDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        var suit = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!Gear.is(suit)) {
            return;
        }
        var totals = Gear.totals(suit);
        if (!totals.has(Stat.SHIELD)) {
            return;
        }
        var hit = Shielding.absorb(event.getNewDamage(), totals.underCeiling(Stat.SHIELD),
                Gear.energy(suit), totals.meanDraw(Stat.SHIELD));
        if (hit.absorbed() > 0) {
            event.setNewDamage(event.getNewDamage() - hit.absorbed());
            Gear.setEnergy(suit, Gear.energy(suit) - hit.spent());
        }
    }
}
