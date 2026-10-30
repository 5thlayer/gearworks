// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import java.util.List;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

/**
 * An item whose stack the server changes in place every tick it is worn or held, as a Suit's FE or a
 * Railgun's Charge does. A copy a tick old differs from it in only those components, and is the same
 * item for the hand's re-equip animation, for a block being broken, and for a slot the creative
 * inventory sends back.
 */
public interface TickingGear {

    /** The components the item's stack changes as it ticks. */
    List<DataComponentType<?>> tickingComponents();

    /** Whether the two stacks differ in nothing but the components their item changes as it ticks. */
    static boolean sameButForTicking(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, withTickingOf(a, b));
    }

    /**
     * {@code incoming}, as set over a slot holding {@code current}, with {@code current}'s ticking
     * components when those are all it differs in: so a copy a tick old neither undoes the ticks since
     * nor counts as putting the item on again.
     */
    static ItemStack keepingTicksOf(ItemStack current, ItemStack incoming) {
        return !current.isEmpty() && current.getCount() == incoming.getCount() && sameButForTicking(current, incoming)
                ? withTickingOf(current, incoming)
                : incoming;
    }

    /** A copy of {@code stack} with {@code from}'s values of the components its item ticks. */
    private static ItemStack withTickingOf(ItemStack from, ItemStack stack) {
        var copy = stack.copy();
        if (stack.getItem() instanceof TickingGear gear) {
            for (var type : gear.tickingComponents()) {
                copyComponent(type, from, copy);
            }
        }
        return copy;
    }

    private static <T> void copyComponent(DataComponentType<T> type, ItemStack from, ItemStack to) {
        to.set(type, from.get(type));
    }
}
