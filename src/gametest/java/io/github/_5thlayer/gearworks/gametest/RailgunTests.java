// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.SuitFixtures.charge;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.suitWith;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.wearer;

import java.util.List;

import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.level.LanceCharge;
import io.github._5thlayer.gearworks.railgun.Railgun;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Railgun: Charged, use fires an instant shot along the player's gaze that hurts every mob in its
 * line until a block stops it, and breaks none; uncharged, use does nothing. Held in either hand it
 * Charges again over {@link GearworksConfig#railgunChargeTicks()} ticks, paying
 * {@link GearworksConfig#railgunChargeEnergy()} as it completes, or waits at full without it; any charger
 * or a worn Suit fills it.
 */
final class RailgunTests {

    /** The player stands at the platform's north edge, looking south down its middle. */
    private static final Vec3 FEET = new Vec3(4.5, 1, 1.5);

    /** Plenty for anything one test does. */
    private static final int FULL = 50_000;

    private RailgunTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("railgun_a_charged_railgun_hurts_a_mob_in_its_sights_and_an_uncharged_one_does_not", 1, RailgunTests::hurts);
        tests.test("railgun_its_shot_pierces_every_mob_in_its_line", 1, RailgunTests::pierces);
        tests.test("railgun_a_block_stops_its_shot_and_stays", 1, RailgunTests::blockStops);
        tests.test("railgun_it_charges_again_in_either_hand_and_only_held", 1, RailgunTests::charges);
        tests.test("railgun_without_its_cost_it_waits_uncharged_until_fe_arrives", 1, RailgunTests::waitsDry);
        tests.test("railgun_quick_charge_three_charges_it_in_ten_ticks", 1, RailgunTests::quickCharge);
        tests.test("railgun_power_raises_its_damage", 1, RailgunTests::power);
        tests.test("railgun_takes_quick_charge_and_power_and_no_other_enchantment", 1, RailgunTests::enchantments);
        tests.test("railgun_holds_fe_through_the_energy_capability", 1, RailgunTests::capability);
        tests.test("railgun_a_worn_suit_tops_it_up_in_either_hand_down_to_its_reserve", 1, RailgunTests::suitTopsUp);
        tests.test("railgun_is_crafted_from_netherite_copper_redstone_and_a_lightning_rod", 1, RailgunTests::crafts);
    }

    private static void hurts(GameTestHelper helper) {
        backWall(helper);
        var villager = villager(helper, 5);
        var player = shooter(helper);
        var railgun = railgun(FULL);
        player.setItemSlot(EquipmentSlot.MAINHAND, railgun);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, villager.getBoundingBox().getCenter());

        fire(helper, player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(villager.getHealth(), villager.getMaxHealth(), Component.literal("a villager's health after an uncharged Railgun's use"));

        chargeUp(helper, player);
        fire(helper, player, InteractionHand.MAIN_HAND);

        helper.assertValueEqual(villager.getMaxHealth() - villager.getHealth(), (float) GearworksConfig.railgunDamage(),
                Component.literal("the damage a shot did a villager"));
        helper.assertTrue(villager.getDeltaMovement().z > 0, Component.literal("the shot did not knock the villager along the beam"));
        helper.assertFalse(Railgun.charged(railgun), Component.literal("the Railgun is still Charged after it fired"));
        helper.succeed();
    }

    private static void pierces(GameTestHelper helper) {
        backWall(helper);
        var near = villager(helper, 4);
        var far = villager(helper, 6);
        var player = shooter(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, railgun(FULL));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, far.getBoundingBox().getCenter());
        chargeUp(helper, player);

        fire(helper, player, InteractionHand.MAIN_HAND);

        for (var villager : List.of(near, far)) {
            helper.assertValueEqual(villager.getMaxHealth() - villager.getHealth(), (float) GearworksConfig.railgunDamage(),
                    Component.literal("the damage the shot did a villager at z = " + villager.getZ()));
        }
        helper.succeed();
    }

    private static void blockStops(GameTestHelper helper) {
        backWall(helper);
        var villager = villager(helper, 5);
        var wall = new BlockPos(4, 2, 3);
        helper.setBlock(wall, Blocks.GLASS);
        var player = shooter(helper);
        player.setItemSlot(EquipmentSlot.MAINHAND, railgun(FULL));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, villager.getBoundingBox().getCenter());
        chargeUp(helper, player);

        fire(helper, player, InteractionHand.MAIN_HAND);

        helper.assertValueEqual(villager.getHealth(), villager.getMaxHealth(), Component.literal("a villager's health behind glass"));
        helper.assertBlockPresent(Blocks.GLASS, wall);
        helper.succeed();
    }

    private static void charges(GameTestHelper helper) {
        backWall(helper);
        var player = shooter(helper);
        var railgun = railgun(FULL);
        player.setItemSlot(EquipmentSlot.MAINHAND, railgun);
        chargeUp(helper, player);
        int cost = GearworksConfig.railgunChargeEnergy();
        helper.assertValueEqual(Railgun.energy(railgun), FULL - cost, Component.literal("the Railgun's FE after its first Charge"));

        // Fired from the main hand, it charges there again in the Charge time, and no sooner.
        fire(helper, player, InteractionHand.MAIN_HAND);
        tick(player, GearworksConfig.railgunChargeTicks() - 1);
        helper.assertFalse(Railgun.charged(railgun), Component.literal("the Railgun is Charged a tick early"));
        tick(player, 1);
        helper.assertTrue(Railgun.charged(railgun), Component.literal("the Railgun is not Charged in the main hand"));
        helper.assertValueEqual(Railgun.energy(railgun), FULL - 2 * cost, Component.literal("the Railgun's FE after its second Charge"));

        // Fired from the off hand, it charges there.
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.OFFHAND, railgun);
        fire(helper, player, InteractionHand.OFF_HAND);
        chargeUp(helper, player);
        helper.assertValueEqual(Railgun.energy(railgun), FULL - 3 * cost, Component.literal("the Railgun's FE after a Charge in the off hand"));

        // Put away partway, it keeps its progress and nothing more, and picks up where it stopped.
        fire(helper, player, InteractionHand.OFF_HAND);
        tick(player, 10);
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        player.getInventory().setItem(10, railgun);
        tick(player, 2 * GearworksConfig.railgunChargeTicks());
        helper.assertFalse(Railgun.charged(railgun), Component.literal("the Railgun charged in the inventory"));
        player.getInventory().setItem(10, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.MAINHAND, railgun);
        tick(player, GearworksConfig.railgunChargeTicks() - 10);
        helper.assertTrue(Railgun.charged(railgun), Component.literal("the Railgun lost its progress while put away"));

        // Charged, it stays so put away.
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.getInventory().setItem(10, railgun);
        tick(player, 5);
        helper.assertTrue(Railgun.charged(railgun), Component.literal("the Railgun lost its Charge in the inventory"));
        helper.assertValueEqual(Railgun.energy(railgun), FULL - 4 * cost, Component.literal("the Railgun's FE after its fourth Charge"));
        helper.succeed();
    }

    private static void waitsDry(GameTestHelper helper) {
        var player = shooter(helper);
        int cost = GearworksConfig.railgunChargeEnergy();
        var railgun = railgun(cost - 1);
        player.setItemSlot(EquipmentSlot.MAINHAND, railgun);

        tick(player, GearworksConfig.railgunChargeTicks() + 5);
        helper.assertFalse(Railgun.charged(railgun), Component.literal("a Railgun short of its cost is Charged"));
        helper.assertValueEqual(Railgun.energy(railgun), cost - 1, Component.literal("the short Railgun's FE"));

        fill(railgun, 1);
        tick(player, 1);
        helper.assertTrue(Railgun.charged(railgun), Component.literal("the Railgun did not charge once its cost arrived"));
        helper.assertValueEqual(Railgun.energy(railgun), 0, Component.literal("the Railgun's FE after it charged on its cost"));
        helper.succeed();
    }

    private static void quickCharge(GameTestHelper helper) {
        var player = shooter(helper);
        var railgun = railgun(FULL);
        railgun.enchant(enchantment(helper, Enchantments.QUICK_CHARGE), 3);
        player.setItemSlot(EquipmentSlot.MAINHAND, railgun);

        tick(player, 9);
        helper.assertFalse(Railgun.charged(railgun), Component.literal("a Quick Charge III Railgun is Charged in 9 ticks"));
        tick(player, 1);
        helper.assertTrue(Railgun.charged(railgun), Component.literal("a Quick Charge III Railgun is not Charged in 10 ticks"));
        helper.succeed();
    }

    private static void power(GameTestHelper helper) {
        backWall(helper);
        var villager = villager(helper, 5);
        var player = shooter(helper);
        var railgun = railgun(FULL);
        railgun.enchant(enchantment(helper, Enchantments.POWER), 1);
        player.setItemSlot(EquipmentSlot.MAINHAND, railgun);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, villager.getBoundingBox().getCenter());
        chargeUp(helper, player);

        fire(helper, player, InteractionHand.MAIN_HAND);

        // Power I: half again the damage, which leaves a villager of 20 dead from 16.
        helper.assertTrue(villager.isDeadOrDying(), Component.literal("a Power I shot left a villager at " + villager.getHealth()));
        helper.succeed();
    }

    private static void enchantments(GameTestHelper helper) {
        var railgun = new ItemStack(GearContent.RAILGUN.get());
        for (var key : List.of(Enchantments.QUICK_CHARGE, Enchantments.POWER)) {
            helper.assertTrue(railgun.supportsEnchantment(enchantment(helper, key)), Component.literal("the Railgun does not take " + key.identifier()));
            helper.assertTrue(railgun.isPrimaryItemFor(enchantment(helper, key)), Component.literal("an enchanting table does not offer " + key.identifier()));
        }
        for (var key : List.of(Enchantments.UNBREAKING, Enchantments.MENDING, Enchantments.MULTISHOT, Enchantments.PIERCING, Enchantments.INFINITY,
                Enchantments.FLAME, Enchantments.PUNCH, Enchantments.SHARPNESS)) {
            helper.assertFalse(railgun.supportsEnchantment(enchantment(helper, key)), Component.literal("the Railgun takes " + key.identifier()));
        }
        helper.assertFalse(railgun.isDamageableItem(), Component.literal("the Railgun has durability"));
        var resistant = railgun.get(DataComponents.DAMAGE_RESISTANT);
        helper.assertTrue(resistant != null && resistant.isResistantTo(helper.getLevel().damageSources().lava()),
                Component.literal("the Railgun burns in lava"));
        helper.succeed();
    }

    private static void capability(GameTestHelper helper) {
        var railgun = new ItemStack(GearContent.RAILGUN.get());
        var energy = ItemAccess.forStack(railgun).getCapability(Capabilities.Energy.ITEM);
        helper.assertTrue(energy != null, Component.literal("the Railgun has no energy capability"));
        helper.assertValueEqual(energy.getCapacityAsLong(), 100_000L, Component.literal("the Railgun's capacity"));
        helper.assertValueEqual(fill(railgun, 200_000), 100_000, Component.literal("FE a charger put in an empty Railgun"));
        helper.assertValueEqual(Railgun.energy(railgun), 100_000, Component.literal("the Railgun's FE"));
        helper.succeed();
    }

    private static void suitTopsUp(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get());
        int capacity = Suit.capacity(suit);
        charge(suit, capacity);
        var player = wearer(helper, suit);
        var railgun = railgun(0);
        int rate = GearworksConfig.netheriteLanceChargeRate();

        player.setItemSlot(EquipmentSlot.MAINHAND, railgun);
        player.tick();
        helper.assertValueEqual(Railgun.energy(railgun), rate, Component.literal("the Railgun's FE after a tick in the main hand"));
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.OFFHAND, railgun);
        player.tick();
        helper.assertValueEqual(Railgun.energy(railgun), 2 * rate, Component.literal("the Railgun's FE after a tick in the off hand"));
        helper.assertValueEqual(Suit.energy(suit), capacity - 2 * rate, Component.literal("the suit's FE after topping up the Railgun"));

        // A suit 300 over its reserve gives 300, and then nothing.
        var low = suitWith(GearContent.BATTERY_UPGRADE.get());
        int reserve = LanceCharge.kept(Suit.capacity(low), GearworksConfig.netheriteLanceChargeReserve());
        charge(low, reserve + 300);
        var wearer = wearer(helper, low);
        var dry = railgun(0);
        wearer.setItemSlot(EquipmentSlot.MAINHAND, dry);
        wearer.tick();
        wearer.tick();
        helper.assertValueEqual(Railgun.energy(dry), 300, Component.literal("the Railgun's FE from a suit 300 over its reserve"));
        helper.assertValueEqual(Suit.energy(low), reserve, Component.literal("the low suit's FE"));
        helper.succeed();
    }

    private static void crafts(GameTestHelper helper) {
        var empty = ItemStack.EMPTY;
        var copper = new ItemStack(Items.COPPER_BLOCK);
        var input = CraftingInput.of(3, 3, List.of(
                empty, new ItemStack(Items.LIGHTNING_ROD), empty,
                copper, new ItemStack(Items.REDSTONE_BLOCK), copper.copy(),
                empty, new ItemStack(Items.NETHERITE_INGOT), empty));
        var level = helper.getLevel();
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        helper.assertTrue(recipe.isPresent(), Component.literal("no recipe makes the Railgun"));
        helper.assertTrue(recipe.get().value().assemble(input).is(GearContent.RAILGUN.get()), Component.literal("the recipe makes something else"));
        helper.succeed();
    }

    /** A player in survival at the platform's north edge, ticked by the test. */
    private static Player shooter(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var feet = helper.absoluteVec(FEET);
        player.setPos(feet.x, feet.y, feet.z);
        return player;
    }

    /** A villager, 20 health and no armor, that stands still in the middle of the platform at that z. */
    private static Villager villager(GameTestHelper helper, int z) {
        return helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(4, 1, z));
    }

    /** Stone across the platform's south edge, so no shot leaves the platform for another test's. */
    private static void backWall(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int y = 1; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, 8), Blocks.STONE);
            }
        }
    }

    /** A Railgun holding that much FE, put in through its capability as a charger would. */
    private static ItemStack railgun(int fe) {
        var railgun = new ItemStack(GearContent.RAILGUN.get());
        fill(railgun, fe);
        return railgun;
    }

    private static int fill(ItemStack railgun, int fe) {
        if (fe <= 0) {
            return 0;
        }
        try (var transaction = Transaction.openRoot()) {
            int inserted = ItemAccess.forStack(railgun).getCapability(Capabilities.Energy.ITEM).insert(fe, transaction);
            transaction.commit();
            return inserted;
        }
    }

    /** Holds the Railgun for its Charge time, after which it must be Charged. */
    private static void chargeUp(GameTestHelper helper, Player player) {
        tick(player, GearworksConfig.railgunChargeTicks());
        var held = player.getMainHandItem().isEmpty() ? player.getOffhandItem() : player.getMainHandItem();
        helper.assertTrue(Railgun.charged(held), Component.literal("the Railgun is not Charged after its Charge time"));
    }

    private static void tick(Player player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            player.tick();
        }
    }

    /** Uses the item in that hand, as a right-click does. */
    private static void fire(GameTestHelper helper, Player player, InteractionHand hand) {
        player.getItemInHand(hand).use(helper.getLevel(), player, hand);
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
