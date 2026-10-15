// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github._5thlayer.gearworks.lance.BoreLance;
import io.github._5thlayer.gearworks.lance.CycleBorePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** The key that steps the Bore Lance in the main hand on to its next Bore. */
final class BoreKey {

    private static final KeyMapping CYCLE = new KeyMapping("key.gearworks.cycle_bore",
            KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, GearworksClient.KEYS);

    private BoreKey() {
    }

    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(CYCLE);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        while (CYCLE.consumeClick()) {
            if (player != null && BoreLance.is(player.getMainHandItem())) {
                ClientPacketDistributor.sendToServer(CycleBorePayload.INSTANCE);
            }
        }
    }
}
