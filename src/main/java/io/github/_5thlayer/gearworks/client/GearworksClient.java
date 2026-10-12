// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import io.github._5thlayer.gearworks.Gearworks;
import io.github._5thlayer.gearworks.gear.GearContent;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client's side of Gear: the Gear Bench's screen, the Gear Grid's screen and the key that opens
 * it, the worn suit's FE on the HUD, the tooltips of suits and upgrades, and the configs and the mod list's screen for them.
 */
@Mod(value = Gearworks.MOD_ID, dist = Dist.CLIENT)
public final class GearworksClient {

    static final KeyMapping.Category KEYS =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, Gearworks.MOD_ID));

    public GearworksClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, GearworksClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.registerCategory(KEYS));
        modBus.addListener(GridKey::onRegisterKeys);
        modBus.addListener(GearworksClient::registerScreens);
        modBus.addListener(EnergyHud::onRegisterLayers);
        NeoForge.EVENT_BUS.addListener(GridKey::onClientTick);
        NeoForge.EVENT_BUS.addListener(GearTooltips::onTooltip);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(GearContent.GRID_MENU.get(), GearGridScreen::new);
        event.register(GearContent.GEAR_BENCH_MENU.get(), GearBenchScreen::new);
    }
}
