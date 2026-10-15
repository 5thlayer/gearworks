// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.gear.OpenGridPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** The key that opens the worn suit's Gear Grid from anywhere. */
final class GridKey {

    private static final KeyMapping OPEN = new KeyMapping("key.gearworks.open_grid",
            KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, GearworksClient.KEYS);

    private GridKey() {
    }

    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        while (OPEN.consumeClick()) {
            if (player != null && Suit.is(player.getItemBySlot(EquipmentSlot.CHEST))) {
                ClientPacketDistributor.sendToServer(OpenGridPayload.INSTANCE);
            }
        }
    }
}
