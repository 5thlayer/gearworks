// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github._5thlayer.gearworks.gear.TickingGear;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A slot the creative inventory sets, kept to the server's ticks: that inventory sends back every slot
 * the server changes, a tick or more late, so a Suit's, Bore Lance's or Railgun's comes back with its
 * FE or Charge of a tick ago. Set as it came, it would undo those ticks and put a Suit on again, with
 * its equip sound, every tick it charges. The client is still taken to hold the stack it sent, so the
 * server sends it the slot's again. No event lets a mod change a creative slot's stack.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @WrapOperation(method = "handleSetCreativeModeSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;setByPlayer(Lnet/minecraft/world/item/ItemStack;)V"))
    private void gearworks$keepTicks(Slot slot, ItemStack stack, Operation<Void> original) {
        original.call(slot, TickingGear.keepingTicksOf(slot.getItem(), stack));
    }
}
