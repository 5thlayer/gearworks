// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github._5thlayer.gearworks.gear.BoostPayload;
import io.github._5thlayer.gearworks.gear.SuitBoost;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * The key that fires the worn suit's Boost, unbound until the player binds it. A press that would not
 * fire, in a Cooldown, short of FE or not flying, fizzles here and never reaches the server.
 */
final class BoostKey {

    private static final KeyMapping BOOST = new KeyMapping("key.gearworks.boost",
            KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, GearworksClient.KEYS);

    private BoostKey() {
    }

    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(BOOST);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        while (BOOST.consumeClick()) {
            if (player == null) {
                continue;
            }
            if (SuitBoost.canFire(player)) {
                ClientPacketDistributor.sendToServer(BoostPayload.INSTANCE);
            } else {
                player.playSound(SoundEvents.DISPENSER_FAIL, 0.5f, 1.2f);
            }
        }
    }
}
