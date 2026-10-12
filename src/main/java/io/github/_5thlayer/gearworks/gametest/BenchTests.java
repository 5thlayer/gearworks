// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.GearFixtures.suitWith;
import static io.github._5thlayer.gearworks.gametest.GearFixtures.wearer;

import io.github._5thlayer.gearworks.bench.GearBenchBlockEntity;
import io.github._5thlayer.gearworks.bench.GearBenchMenu;
import io.github._5thlayer.gearworks.gear.Gear;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearGridMenu;
import io.github._5thlayer.gearworks.gear.GearUpgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Gear Bench raises one Level of the upgrade it is set to per click, once automation has stocked
 * the whole of that Level's cost, and spends exactly that cost. gametest_pack's data makes a redstone
 * block a one-cell battery of 1234 FE plus 1000 per Level, whose Level costs one iron ingot and 100 FE,
 * both doubling each Level, so every number here comes from a datapack.
 */
final class BenchTests {

    private static final BlockPos BENCH = new BlockPos(1, 1, 1);
    private static final BlockPos HOPPER = BENCH.above();

    private BenchTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("a_hopper_stocks_the_bench_and_each_click_raises_one_level_and_spends_its_cost", 200, BenchTests::hopperFed);
        tests.test("a_click_short_of_the_fe_does_nothing", 1, BenchTests::shortOfFe);
        tests.test("the_bench_stocks_only_what_the_next_level_costs_and_gives_nothing_back", 1, BenchTests::stocksOnlyTheCost);
        tests.test("a_raised_level_stays_on_the_upgrade_in_another_suit", 1, BenchTests::levelTravels);
        tests.test("gearworks_own_upgrades_cost_tagged_materials_and_fe", 1, BenchTests::defaultCosts);
    }

    private static void hopperFed(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = benchWith(helper, player, suitWith(Items.REDSTONE_BLOCK));
        helper.setBlock(HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.getBlockEntity(HOPPER, HopperBlockEntity.class).setItem(0, new ItemStack(Items.IRON_INGOT, 3));
        helper.assertValueEqual(charge(helper, 1000), 100, Component.literal("FE the first Level takes"));

        helper.assertFalse(menu.clickMenuButton(player, GearBenchMenu.RAISE), Component.literal("raised with no iron stocked"));
        helper.assertValueEqual(bench(helper).energy(), 100L, Component.literal("FE after a click short of the cost"));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertValueEqual(menu.stocked(0), 1L, Component.literal("iron stocked")))
                // The hopper keeps its other two: the bench takes no more than the Level costs.
                .thenExecuteAfter(20, () -> {
                    helper.assertValueEqual(menu.stocked(0), 1L, Component.literal("iron stocked a while later"));
                    helper.assertTrue(menu.clickMenuButton(player, GearBenchMenu.RAISE), Component.literal("the click raised nothing"));
                    helper.assertValueEqual(level(helper), 1, Component.literal("Level after one click"));
                    helper.assertValueEqual(menu.stocked(0), 0L, Component.literal("iron left after the Level"));
                    helper.assertValueEqual(bench(helper).energy(), 0L, Component.literal("FE left after the Level"));

                    helper.assertFalse(menu.clickMenuButton(player, GearBenchMenu.RAISE), Component.literal("a second click raised again"));
                    helper.assertValueEqual(level(helper), 1, Component.literal("Level after a click short of the cost"));
                    helper.assertValueEqual(charge(helper, 1000), 200, Component.literal("FE the second Level takes"));
                })
                .thenWaitUntil(() -> helper.assertValueEqual(menu.stocked(0), 2L, Component.literal("iron stocked for the second Level")))
                .thenExecute(() -> {
                    helper.assertTrue(menu.clickMenuButton(player, GearBenchMenu.RAISE), Component.literal("the second click raised nothing"));
                    helper.assertValueEqual(level(helper), 2, Component.literal("Level after two clicks"));
                    helper.assertValueEqual(bench(helper).energy(), 0L, Component.literal("FE left after the second Level"));
                    helper.assertTrue(helper.getBlockEntity(HOPPER, HopperBlockEntity.class).isEmpty(),
                            Component.literal("the hopper still holds iron"));
                    helper.assertValueEqual(Gear.capacity(bench(helper).suit()), 1234 + 2 * 1000, Component.literal("capacity at Level 2"));
                })
                .thenSucceed();
    }

    private static void shortOfFe(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = benchWith(helper, player, suitWith(Items.REDSTONE_BLOCK));
        helper.assertValueEqual(stock(helper, new ItemStack(Items.IRON_INGOT)), 1, Component.literal("iron taken"));
        charge(helper, 50);

        helper.assertFalse(menu.clickMenuButton(player, GearBenchMenu.RAISE), Component.literal("raised short of the FE"));

        helper.assertValueEqual(level(helper), 0, Component.literal("Level"));
        helper.assertValueEqual(menu.stocked(0), 1L, Component.literal("iron stocked"));
        helper.assertValueEqual(bench(helper).energy(), 50L, Component.literal("FE stocked"));
        helper.succeed();
    }

    private static void stocksOnlyTheCost(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        benchWith(helper, player, suitWith(Items.REDSTONE_BLOCK));

        helper.assertValueEqual(stock(helper, new ItemStack(Items.DIRT, 5)), 0, Component.literal("dirt taken"));
        helper.assertValueEqual(stock(helper, new ItemStack(Items.IRON_INGOT, 5)), 1, Component.literal("iron taken"));
        helper.assertValueEqual(charge(helper, 1000), 100, Component.literal("FE taken"));

        var items = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(BENCH), null);
        var energy = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(BENCH), null);
        try (var transaction = Transaction.openRoot()) {
            helper.assertValueEqual(items.extract(ItemResource.of(Items.IRON_INGOT), 1, transaction), 0, Component.literal("iron given back"));
            helper.assertValueEqual(energy.extract(100, transaction), 0, Component.literal("FE given back"));
        }
        helper.succeed();
    }

    private static void levelTravels(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = benchWith(helper, player, suitWith(Items.REDSTONE_BLOCK));
        stock(helper, new ItemStack(Items.IRON_INGOT));
        charge(helper, 100);
        helper.assertTrue(menu.clickMenuButton(player, GearBenchMenu.RAISE), Component.literal("the click raised nothing"));
        var raised = menu.getSlot(GearBenchMenu.SUIT_SLOT).remove(1);

        var wearer = wearer(helper, raised);
        var first = new GearGridMenu(1, wearer.getInventory());
        helper.assertTrue(first.clickMenuButton(wearer, GearGridMenu.cellButton(0, 0)), Component.literal("nothing picked up"));
        var upgrade = first.getCarried();
        first.setCarried(ItemStack.EMPTY);
        helper.assertValueEqual(GearUpgrade.level(upgrade), 1, Component.literal("Level of the picked-up upgrade"));

        wearer.setItemSlot(EquipmentSlot.CHEST, suitWith());
        var second = new GearGridMenu(2, wearer.getInventory());
        second.setCarried(upgrade);
        helper.assertTrue(second.clickMenuButton(wearer, GearGridMenu.cellButton(2, 2)), Component.literal("nothing placed"));

        var other = wearer.getItemBySlot(EquipmentSlot.CHEST);
        helper.assertValueEqual(GearUpgrade.level(Gear.grid(other).at(2, 2).orElseThrow().item()), 1, Component.literal("Level in the other suit"));
        helper.assertValueEqual(Gear.capacity(other), 1234 + 1000, Component.literal("the other suit's capacity"));
        helper.succeed();
    }

    private static void defaultCosts(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = benchWith(helper, player, suitWith(GearContent.SPEED_UPGRADE.get()));

        var cost = menu.target().orElseThrow().cost();
        helper.assertTrue(cost != null, Component.literal("the speed upgrade has no Level cost"));
        helper.assertTrue(cost.energy() > 0, Component.literal("the first Level costs no FE"));
        helper.assertTrue(!cost.materials().isEmpty(), Component.literal("the first Level costs no materials"));
        helper.assertTrue(stock(helper, new ItemStack(Items.IRON_INGOT)) == 1, Component.literal("iron is not a material of the first Level"));
        helper.succeed();
    }

    /** A bench at {@link #BENCH} holding the suit and set to its top-left upgrade, and its menu. */
    private static GearBenchMenu benchWith(GameTestHelper helper, Player player, ItemStack suit) {
        helper.setBlock(BENCH, GearContent.GEAR_BENCH.get());
        var menu = (GearBenchMenu) bench(helper).createMenu(1, player.getInventory(), player);
        menu.getSlot(GearBenchMenu.SUIT_SLOT).set(suit);
        helper.assertTrue(menu.clickMenuButton(player, GearGridMenu.cellButton(0, 0)), Component.literal("no upgrade to select"));
        return menu;
    }

    private static GearBenchBlockEntity bench(GameTestHelper helper) {
        return helper.getBlockEntity(BENCH, GearBenchBlockEntity.class);
    }

    private static int level(GameTestHelper helper) {
        return GearUpgrade.level(Gear.grid(bench(helper).suit()).at(0, 0).orElseThrow().item());
    }

    /** Fills the bench through its item capability, as another mod's pipe does, and gives back how many it took. */
    private static int stock(GameTestHelper helper, ItemStack stack) {
        var items = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(BENCH), Direction.UP);
        try (var transaction = Transaction.openRoot()) {
            int inserted = items.insert(ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
            return inserted;
        }
    }

    /** Fills the bench through its energy capability, as another mod's cable does, and gives back how much it took. */
    private static int charge(GameTestHelper helper, int amount) {
        var energy = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(BENCH), Direction.UP);
        try (var transaction = Transaction.openRoot()) {
            int inserted = energy.insert(amount, transaction);
            transaction.commit();
            return inserted;
        }
    }
}
