// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import io.github._5thlayer.gearworks.Gearworks;
import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.gear.SuitBoost;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The worn suit's FE against what its batteries hold, on the HUD without opening the Gear Grid: a bar
 * and its numbers in the corner the client config names, and while a Boost cools down, a thin bar of
 * the Cooldown left past it. A suit with no battery has nothing to show, and the client config or F1
 * hides it.
 */
final class EnergyHud {

    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "energy");
    private static final int FRAME = 0xFF000000;
    private static final int EMPTY = 0xFF3F3F3F;
    private static final int FULL = 0xFF3FBFDF;
    private static final int TEXT = 0xFFFFFFFF;
    // The suits' orange core, as a firework's spark.
    private static final int COOLDOWN = 0xFFF07A24;

    private EnergyHud() {
    }

    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EFFECTS, LAYER, EnergyHud::extract);
    }

    private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        // A modded layer gets no hide-GUI check of its own: F1 hides it here as it does vanilla's.
        if (minecraft.options.hideGui || player == null || !GearworksClientConfig.showEnergy()) {
            return;
        }
        var suit = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!Suit.is(suit) || Suit.capacity(suit) <= 0) {
            return;
        }
        var font = minecraft.font;
        var text = FeText.reading(suit);
        var layout = EnergyHudLayout.place(GearworksClientConfig.energyCorner(),
                graphics.guiWidth(), graphics.guiHeight(), font.width(text), font.lineHeight);

        int x = layout.barX();
        int y = layout.barY();
        graphics.fill(x, y, x + EnergyHudLayout.BAR_WIDTH, y + EnergyHudLayout.BAR_HEIGHT, FRAME);
        graphics.fill(x + 1, y + 1, x + EnergyHudLayout.BAR_WIDTH - 1, y + EnergyHudLayout.BAR_HEIGHT - 1, EMPTY);
        int filled = EnergyHudLayout.filled(Suit.energy(suit), Suit.capacity(suit));
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + EnergyHudLayout.BAR_HEIGHT - 1, FULL);
        int cooling = EnergyHudLayout.cooldownFilled(SuitBoost.of(player).cooldownFraction());
        if (cooling > 0) {
            int cooldownY = layout.cooldownY(GearworksClientConfig.energyCorner());
            graphics.fill(x, cooldownY, x + EnergyHudLayout.BAR_WIDTH, cooldownY + EnergyHudLayout.COOLDOWN_HEIGHT, FRAME);
            graphics.fill(x + 1, cooldownY + 1, x + 1 + cooling, cooldownY + EnergyHudLayout.COOLDOWN_HEIGHT - 1, COOLDOWN);
        }
        graphics.text(font, text, layout.textX(), layout.textY(), TEXT, true);
    }
}
