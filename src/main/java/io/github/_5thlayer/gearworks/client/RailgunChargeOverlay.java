// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.railgun.Railgun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

/**
 * A Railgun's slot shaded over the part of its Charge left to load, falling as it loads, as vanilla
 * shades an item on cooldown; a Charged Railgun's is clear.
 */
final class RailgunChargeOverlay implements IItemDecorator {

    private RailgunChargeOverlay() {
    }

    static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(GearContent.RAILGUN.get(), new RailgunChargeOverlay());
    }

    @Override
    public boolean render(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        var level = Minecraft.getInstance().level;
        float left = level == null ? 0 : Railgun.chargeLeft(level, stack);
        if (left > 0) {
            int top = y + Mth.floor(16.0F * (1.0F - left));
            graphics.fill(RenderPipelines.GUI, x, top, x + 16, top + Mth.ceil(16.0F * left), Integer.MAX_VALUE);
        }
        return false;
    }
}
