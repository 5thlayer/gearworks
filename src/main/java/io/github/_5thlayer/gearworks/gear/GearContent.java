// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import java.util.Set;

import io.github._5thlayer.gearworks.Gearworks;
import io.github._5thlayer.gearworks.bench.GearBenchBlock;
import io.github._5thlayer.gearworks.bench.GearBenchBlockEntity;
import io.github._5thlayer.gearworks.bench.GearBenchMenu;
import io.github._5thlayer.libworks.WorksTab;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;

/**
 * What Gearworks registers: the suit's three tiers, the upgrades, the components the suit and its upgrades
 * save, the Gear Grid's menu, the Gear Bench, and the data maps that make an item an upgrade, size
 * each tier's grid and price each upgrade's Levels.
 */
public final class GearContent {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Gearworks.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Gearworks.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Gearworks.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Gearworks.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Gearworks.MOD_ID);

    /** The upgrades a suit's Gear Grid holds and where. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GridContents>> GRID = COMPONENTS.registerComponentType(
            "gear_grid", builder -> builder.persistent(GridContents.CODEC).networkSynchronized(GridContents.STREAM_CODEC));

    /** The FE a suit holds, in its batteries. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = COMPONENTS.registerComponentType(
            "energy", builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /**
     * The Level a Gear Upgrade carries, which a Gear Bench raises. It is the upgrade's own, so it goes
     * with the upgrade from one suit to another; an upgrade with none is at Level 0.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LEVEL = COMPONENTS.registerComponentType(
            "level", builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Tier 1, with an iron chestplate's armor. */
    public static final DeferredItem<GearItem> IRON_GEAR = ITEMS.registerItem("iron_gear", GearItem::new,
            properties -> suit(properties, ArmorMaterials.IRON, "iron_gear"));

    /** Tier 2, with a diamond chestplate's armor, crafted from tier 1. */
    public static final DeferredItem<GearItem> DIAMOND_GEAR = ITEMS.registerItem("diamond_gear", GearItem::new,
            properties -> suit(properties, ArmorMaterials.DIAMOND, "diamond_gear"));

    /** Tier 3, with a netherite chestplate's armor, and like it unharmed by fire and lava; smithed from tier 2. */
    public static final DeferredItem<GearItem> NETHERITE_GEAR = ITEMS.registerItem("netherite_gear", GearItem::new,
            properties -> suit(properties, ArmorMaterials.NETHERITE, "netherite_gear").fireResistant());

    public static final DeferredItem<Item> BATTERY_UPGRADE = ITEMS.registerSimpleItem("battery_upgrade");
    public static final DeferredItem<Item> SOLAR_UPGRADE = ITEMS.registerSimpleItem("solar_upgrade");
    public static final DeferredItem<Item> SPEED_UPGRADE = ITEMS.registerSimpleItem("speed_upgrade");
    public static final DeferredItem<Item> SHIELD_UPGRADE = ITEMS.registerSimpleItem("shield_upgrade");
    public static final DeferredItem<Item> JUMP_UPGRADE = ITEMS.registerSimpleItem("jump_upgrade");
    public static final DeferredItem<Item> NIGHT_VISION_UPGRADE = ITEMS.registerSimpleItem("night_vision_upgrade");

    public static final DeferredHolder<MenuType<?>, MenuType<GearGridMenu>> GRID_MENU =
            MENUS.register("gear_grid", () -> IMenuTypeExtension.create(GearGridMenu::new));

    /** Where an upgrade's Level is raised: a placed block holding a suit. */
    public static final DeferredBlock<GearBenchBlock> GEAR_BENCH = BLOCKS.registerBlock("gear_bench", GearBenchBlock::new,
            properties -> properties.mapColor(MapColor.METAL).strength(3.5f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> GEAR_BENCH_ITEM = ITEMS.registerSimpleBlockItem(GEAR_BENCH);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GearBenchBlockEntity>> GEAR_BENCH_ENTITY =
            BLOCK_ENTITIES.register("gear_bench", () -> new BlockEntityType<>(GearBenchBlockEntity::new, Set.of(GEAR_BENCH.get())));

    public static final DeferredHolder<MenuType<?>, MenuType<GearBenchMenu>> GEAR_BENCH_MENU =
            MENUS.register("gear_bench", () -> IMenuTypeExtension.create(GearBenchMenu::new));

    private GearContent() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        MENUS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(GearContent::registerDataMaps);
        modBus.addListener(GearContent::registerCapabilities);
        modBus.addListener(GearContent::fillCreativeTabs);
        modBus.addListener(GearContent::registerPayloads);
        NeoForge.EVENT_BUS.addListener(GearTicker::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(GearTicker::onJump);
        NeoForge.EVENT_BUS.addListener(GearShield::onDamage);
    }

    /**
     * The chestplate of its material, with neither durability nor a way to break: a suit that broke
     * would lose its grid. It is worn in the equipment asset of its own name, not its material's.
     */
    private static Item.Properties suit(Item.Properties properties, ArmorMaterial material, String name) {
        return properties.stacksTo(1)
                .attributes(material.createAttributes(ArmorType.CHESTPLATE))
                .enchantable(material.enchantmentValue())
                .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                        .setEquipSound(material.equipSound())
                        .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, name)))
                        .setDamageOnHurt(false)
                        .build());
    }

    private static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(GearUpgrade.DATA_MAP);
        event.register(GridSize.DATA_MAP);
        event.register(LevelCost.DATA_MAP);
    }

    /**
     * Any mod's charger fills the suit, over the sum of its batteries; any mod's pipes, hoppers and
     * cables stock a Gear Bench, on every face.
     */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Energy.ITEM,
                (stack, access) -> new ItemAccessEnergyHandler(access, ENERGY.get(), Gear.capacity(stack)),
                IRON_GEAR, DIAMOND_GEAR, NETHERITE_GEAR);
        event.registerBlockEntity(Capabilities.Item.BLOCK, GEAR_BENCH_ENTITY.get(), (bench, side) -> bench.stockFace());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, GEAR_BENCH_ENTITY.get(), (bench, side) -> bench.energyFace());
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(OpenGridPayload.TYPE, OpenGridPayload.STREAM_CODEC, OpenGridPayload::handle);
    }

    /** Gearworks' items, in the Works tab every -works Module shares, and in no vanilla tab. */
    private static void fillCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == WorksTab.KEY) {
            event.accept(GEAR_BENCH_ITEM);
            event.accept(IRON_GEAR);
            event.accept(DIAMOND_GEAR);
            event.accept(NETHERITE_GEAR);
            event.accept(BATTERY_UPGRADE);
            event.accept(SOLAR_UPGRADE);
            event.accept(SPEED_UPGRADE);
            event.accept(SHIELD_UPGRADE);
            event.accept(JUMP_UPGRADE);
            event.accept(NIGHT_VISION_UPGRADE);
        }
    }
}
