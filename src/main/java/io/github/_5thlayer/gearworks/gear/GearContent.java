// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import com.mojang.serialization.Codec;
import io.github._5thlayer.gearworks.Gearworks;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;

/**
 * What Gearworks registers: the suit, the first upgrades, the components the suit saves, the Gear
 * Grid's menu, and the data maps that make an item an upgrade and size each tier's grid.
 */
public final class GearContent {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gearworks.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Gearworks.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Gearworks.MOD_ID);

    /** The upgrades a suit's Gear Grid holds and where. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GridContents>> GRID = COMPONENTS.registerComponentType(
            "gear_grid", builder -> builder.persistent(GridContents.CODEC).networkSynchronized(GridContents.STREAM_CODEC));

    /** The FE a suit holds, in its batteries. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = COMPONENTS.registerComponentType(
            "energy", builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Tier 1, with an iron chestplate's armor. */
    public static final DeferredItem<GearItem> IRON_GEAR = ITEMS.registerItem("iron_gear", GearItem::new,
            properties -> suit(properties, ArmorMaterials.IRON));

    public static final DeferredItem<Item> BATTERY_UPGRADE = ITEMS.registerSimpleItem("battery_upgrade");
    public static final DeferredItem<Item> SOLAR_UPGRADE = ITEMS.registerSimpleItem("solar_upgrade");
    public static final DeferredItem<Item> SPEED_UPGRADE = ITEMS.registerSimpleItem("speed_upgrade");

    public static final DeferredHolder<MenuType<?>, MenuType<GearGridMenu>> GRID_MENU =
            MENUS.register("gear_grid", () -> IMenuTypeExtension.create(GearGridMenu::new));

    private GearContent() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(GearContent::registerDataMaps);
        modBus.addListener(GearContent::registerCapabilities);
        modBus.addListener(GearContent::fillCreativeTabs);
        modBus.addListener(GearContent::registerPayloads);
        NeoForge.EVENT_BUS.addListener(GearTicker::onPlayerTick);
    }

    /**
     * The chestplate of its material, with neither durability nor a way to break: a suit that broke
     * would lose its grid.
     */
    private static Item.Properties suit(Item.Properties properties, ArmorMaterial material) {
        return properties.stacksTo(1)
                .attributes(material.createAttributes(ArmorType.CHESTPLATE))
                .enchantable(material.enchantmentValue())
                .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                        .setEquipSound(material.equipSound())
                        .setAsset(material.assetId())
                        .setDamageOnHurt(false)
                        .build());
    }

    private static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(GearUpgrade.DATA_MAP);
        event.register(GridSize.DATA_MAP);
    }

    /** Any mod's charger fills the suit, over the sum of its batteries. */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Energy.ITEM,
                (stack, access) -> new ItemAccessEnergyHandler(access, ENERGY.get(), Gear.capacity(stack)),
                IRON_GEAR);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(OpenGridPayload.TYPE, OpenGridPayload.STREAM_CODEC, OpenGridPayload::handle);
    }

    private static void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(IRON_GEAR);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(BATTERY_UPGRADE);
            event.accept(SOLAR_UPGRADE);
            event.accept(SPEED_UPGRADE);
        }
    }
}
