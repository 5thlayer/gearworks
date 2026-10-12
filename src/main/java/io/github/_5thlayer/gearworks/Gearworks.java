// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks;

import io.github._5thlayer.gearworks.gametest.GearworksGameTests;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * The Library's entry point: the Gear and its upgrades, the server config, and the game tests, which
 * exist only when game tests are enabled.
 */
@Mod(Gearworks.MOD_ID)
public final class Gearworks {

    /** The mod id, which gradle.properties' {@code mod_id} must match. */
    public static final String MOD_ID = "gearworks";

    public Gearworks(IEventBus modBus, ModContainer container) {
        GearContent.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, GearworksConfig.SPEC);
        GearworksGameTests.register(modBus);
    }
}
