// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks;

import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * The Module's entry point: the Suit and its upgrades, and the server config. The game tests hook
 * themselves in from the {@code gametest} source set, which never ships (libworks ADR 0011).
 */
@Mod(Gearworks.MOD_ID)
public final class Gearworks {

    /** The mod id, which gradle.properties' {@code mod_id} must match. */
    public static final String MOD_ID = "gearworks";

    public Gearworks(IEventBus modBus, ModContainer container) {
        GearContent.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, GearworksConfig.SPEC);
    }
}
