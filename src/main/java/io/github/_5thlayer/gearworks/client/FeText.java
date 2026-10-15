// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import io.github._5thlayer.gearworks.gear.Suit;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * FE in the language the client has selected, so a suit's FE reads the same on the HUD, its tooltip
 * and its Gear Grid.
 */
final class FeText {

    private FeText() {
    }

    /** The suit's stored FE against what its batteries hold. */
    static MutableComponent reading(ItemStack suit) {
        return Component.translatable("tooltip.gearworks.energy", fe(Suit.energy(suit)), fe(Suit.capacity(suit)));
    }

    private static String fe(int fe) {
        return FeFormat.format(fe, FeFormat.locale(Minecraft.getInstance().getLanguageManager().getSelected()));
    }
}
