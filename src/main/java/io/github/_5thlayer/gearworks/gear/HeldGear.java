// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import io.github._5thlayer.gearworks.lance.BoreLance;
import io.github._5thlayer.gearworks.level.LanceCharge;
import io.github._5thlayer.gearworks.railgun.Railgun;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The held Gear that runs on FE, the netherite Bore Lance and the Railgun, which a worn Suit tops up in
 * either hand under one rule, {@link LanceCharge}'s, each up to what it holds.
 */
public final class HeldGear {

    private HeldGear() {
    }

    /** Whether the stack is held Gear that runs on FE. */
    static boolean runsOnEnergy(ItemStack stack) {
        return BoreLance.runsOnEnergy(stack) || Railgun.is(stack);
    }

    /**
     * Charges the Gear on FE in either of the player's hands from a worn Suit holding that FE of what its
     * batteries hold, through each one's FE capability as any charger does; gives back the FE the Suit has left.
     */
    static int charge(Player player, int suitEnergy, int suitCapacity) {
        for (var hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (runsOnEnergy(stack)) {
                suitEnergy -= topUp(stack, suitEnergy, suitCapacity);
            }
        }
        return suitEnergy;
    }

    /**
     * Tops up one stack on FE from a Suit holding that FE of what its batteries hold, by the rule above,
     * and gives back how much went in: none into a stack with no FE capability.
     */
    public static int topUp(ItemStack stack, int suitEnergy, int suitCapacity) {
        var handler = ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM);
        if (handler == null) {
            return 0;
        }
        int moved = LanceCharge.transfer(suitEnergy, suitCapacity, handler.getAmountAsInt(), handler.getCapacityAsInt(),
                GearworksConfig.netheriteLanceChargeRate(), GearworksConfig.netheriteLanceChargeReserve());
        if (moved <= 0) {
            return 0;
        }
        try (var transaction = Transaction.openRoot()) {
            int added = handler.insert(moved, transaction);
            transaction.commit();
            return added;
        }
    }
}
