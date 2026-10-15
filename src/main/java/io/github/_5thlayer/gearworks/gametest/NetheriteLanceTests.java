// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.SuitFixtures.charge;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.suitWith;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.wearer;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.lance.Bore;
import io.github._5thlayer.gearworks.lance.BoreLance;
import io.github._5thlayer.gearworks.level.LanceCharge;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The netherite Bore Lance: smithed from the diamond tier, mining at netherite's level in Bores up to
 * 5×3×3, with Lunge III and no food gate, fire resistant and with no durability. It runs on FE, which
 * any charger or a worn Suit fills: a lunge costs {@link GearworksConfig#netheriteLanceLungeEnergy()} a level, a
 * Bore's block {@link GearworksConfig#netheriteLanceBoreEnergy()} and a block of Lunge Boring
 * {@link GearworksConfig#netheriteLanceLungeBoringEnergy()}. Dry, it is a spear that mines single blocks.
 */
final class NetheriteLanceTests {

    /** The cube's near face is at z = 3, its centre block at (4, 2, 3); the player stands north of it. */
    private static final BlockPos CENTRE = new BlockPos(4, 2, 3);

    /** Plenty for anything one test does. */
    private static final int CHARGED = 500_000;

    private NetheriteLanceTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("smithing_the_netherite_lance_keeps_its_enchantments_but_unbreaking_mending_and_wear", 1, NetheriteLanceTests::smiths);
        tests.test("the_netherite_lance_mines_at_netherites_level", 1, NetheriteLanceTests::minesToItsLevel);
        tests.test("the_netherite_lance_hits_harder_than_a_netherite_spear", 1, NetheriteLanceTests::hitsHarder);
        tests.test("the_netherite_lance_is_fire_resistant_and_has_no_durability", 1, NetheriteLanceTests::noDurability);
        tests.test("the_netherite_lance_holds_fe_through_the_energy_capability", 1, NetheriteLanceTests::capability);
        tests.test("the_netherite_lance_bores_five_by_three_by_three_for_fe_a_block", 1, NetheriteLanceTests::bores);
        tests.test("a_netherite_lance_running_dry_stops_its_bore", 1, NetheriteLanceTests::runsDryPartway);
        tests.test("a_dry_netherite_lance_mines_single_blocks_and_bores_nothing", 1, NetheriteLanceTests::dryMines);
        tests.test("the_netherite_lance_has_lunge_three_for_fe_a_level", 1, NetheriteLanceTests::lunges);
        tests.test("the_netherite_lance_lunges_with_any_food", 1, NetheriteLanceTests::noFoodGate);
        tests.test("a_dry_netherite_lance_does_not_lunge", 1, NetheriteLanceTests::dryNoLunge);
        tests.test("lunge_boring_costs_the_netherite_lance_fe_a_block", 1, NetheriteLanceTests::lungeBoring);
        tests.test("a_worn_suit_charges_the_netherite_lance_in_either_hand", 1, NetheriteLanceTests::suitCharges);
        tests.test("a_suit_at_its_reserve_charges_no_lance", 1, NetheriteLanceTests::suitKeepsItsReserve);
        tests.test("charging_the_netherite_lance_neither_resets_mining_nor_reequips_it", 1, NetheriteLanceTests::chargingKeepsTheHand);
    }

    private static void smiths(GameTestHelper helper) {
        var diamond = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        diamond.enchant(enchantment(helper, Enchantments.FORTUNE), 3);
        diamond.enchant(enchantment(helper, Enchantments.UNBREAKING), 3);
        diamond.enchant(enchantment(helper, Enchantments.MENDING), 1);
        diamond.set(GearContent.BORE.get(), Bore.ONE);
        diamond.setDamageValue(100);
        var input = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), diamond, new ItemStack(Items.NETHERITE_INGOT));
        var level = helper.getLevel();
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, level);
        helper.assertTrue(recipe.isPresent(), Component.literal("no recipe makes the netherite lance from the diamond lance"));

        var netherite = recipe.get().value().assemble(input);

        helper.assertTrue(netherite.is(GearContent.NETHERITE_BORE_LANCE.get()), Component.literal("the recipe makes " + netherite));
        helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(enchantment(helper, Enchantments.FORTUNE), netherite), 3,
                Component.literal("the netherite lance's Fortune"));
        helper.assertValueEqual(BoreLance.bore(netherite), Bore.ONE, Component.literal("the netherite lance's Bore"));
        helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(enchantment(helper, Enchantments.UNBREAKING), netherite), 0,
                Component.literal("the netherite lance's Unbreaking"));
        helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(enchantment(helper, Enchantments.MENDING), netherite), 0,
                Component.literal("the netherite lance's Mending"));
        helper.assertFalse(netherite.has(DataComponents.DAMAGE), Component.literal("the netherite lance keeps the diamond lance's wear"));
        helper.succeed();
    }

    private static void minesToItsLevel(GameTestHelper helper) {
        var lance = new ItemStack(GearContent.NETHERITE_BORE_LANCE.get());
        helper.assertTrue(lance.isCorrectToolForDrops(Blocks.ANCIENT_DEBRIS.defaultBlockState()), Component.literal("the netherite lance can't mine ancient debris"));
        helper.assertTrue(lance.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), Component.literal("the netherite lance can't mine obsidian"));
        helper.assertValueEqual(lance.getDestroySpeed(Blocks.STONE.defaultBlockState()), 9.0f, Component.literal("the netherite lance's speed on stone"));
        helper.assertValueEqual(GearContent.NETHERITE_BORE_LANCE.get().widest(), Bore.FIVE, Component.literal("the netherite lance's widest Bore"));
        helper.succeed();
    }

    private static void hitsHarder(GameTestHelper helper) {
        var lance = charged(0);
        helper.assertTrue(attackDamage(lance) > attackDamage(new ItemStack(Items.NETHERITE_SPEAR)),
                Component.literal("the dry netherite lance hits no harder than a netherite spear"));
        helper.assertTrue(lance.get(DataComponents.KINETIC_WEAPON).damageMultiplier()
                        > new ItemStack(Items.NETHERITE_SPEAR).get(DataComponents.KINETIC_WEAPON).damageMultiplier(),
                Component.literal("the netherite lance's charge hits no harder than a netherite spear's"));
        helper.succeed();
    }

    private static double attackDamage(ItemStack stack) {
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .compute(Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND);
    }

    private static void noDurability(GameTestHelper helper) {
        var lance = new ItemStack(GearContent.NETHERITE_BORE_LANCE.get());
        var resistant = lance.get(DataComponents.DAMAGE_RESISTANT);
        helper.assertTrue(resistant != null && resistant.isResistantTo(helper.getLevel().damageSources().lava()),
                Component.literal("the netherite lance burns in lava"));
        helper.assertFalse(lance.isDamageableItem(), Component.literal("the netherite lance has durability"));
        helper.assertFalse(lance.supportsEnchantment(enchantment(helper, Enchantments.UNBREAKING)), Component.literal("the netherite lance takes Unbreaking"));
        helper.assertFalse(lance.supportsEnchantment(enchantment(helper, Enchantments.MENDING)), Component.literal("the netherite lance takes Mending"));
        helper.succeed();
    }

    private static void capability(GameTestHelper helper) {
        var lance = new ItemStack(GearContent.NETHERITE_BORE_LANCE.get());
        var energy = ItemAccess.forStack(lance).getCapability(Capabilities.Energy.ITEM);
        helper.assertTrue(energy != null, Component.literal("the netherite lance has no energy capability"));
        helper.assertValueEqual(energy.getCapacityAsLong(), 1_000_000L, Component.literal("the netherite lance's capacity"));
        int inserted;
        try (var transaction = Transaction.openRoot()) {
            inserted = energy.insert(2_000_000, transaction);
            transaction.commit();
        }
        helper.assertValueEqual(inserted, 1_000_000, Component.literal("FE a charger put in an empty lance"));
        helper.assertValueEqual(BoreLance.energy(lance), 1_000_000, Component.literal("the lance's FE"));
        helper.assertTrue(ItemAccess.forStack(new ItemStack(GearContent.DIAMOND_BORE_LANCE.get())).getCapability(Capabilities.Energy.ITEM) == null,
                Component.literal("the diamond lance has an energy capability"));
        helper.succeed();
    }

    private static void bores(GameTestHelper helper) {
        cube(helper);
        var lance = charged(CHARGED);
        var player = miner(helper, lance);

        mine(helper, player, CENTRE);

        assertBroken(helper, cubeBlocks());
        helper.assertValueEqual(BoreLance.energy(lance), CHARGED - 45 * GearworksConfig.netheriteLanceBoreEnergy(),
                Component.literal("the lance's FE after a Bore of 45 blocks"));
        helper.succeed();
    }

    private static void runsDryPartway(GameTestHelper helper) {
        cube(helper);
        var lance = charged(10 * GearworksConfig.netheriteLanceBoreEnergy());
        var player = miner(helper, lance);

        mine(helper, player, CENTRE);

        int broken = 0;
        for (var block : cubeBlocks()) {
            broken += helper.getBlockState(block).isAir() ? 1 : 0;
        }
        helper.assertValueEqual(broken, 10, Component.literal("blocks a lance with FE for ten broke"));
        helper.assertValueEqual(BoreLance.energy(lance), 0, Component.literal("the lance's FE"));
        helper.succeed();
    }

    private static void dryMines(GameTestHelper helper) {
        cube(helper);
        var lance = charged(0);
        var player = miner(helper, lance);

        mine(helper, player, CENTRE);

        for (var block : cubeBlocks()) {
            helper.assertValueEqual(helper.getBlockState(block).isAir(), block.equals(CENTRE),
                    Component.literal("whether " + block.toShortString() + " is broken"));
        }
        helper.assertTrue(player.getMainHandItem() == lance, Component.literal("the dry lance left the hand"));
        helper.assertValueEqual(lance.getDestroySpeed(Blocks.STONE.defaultBlockState()), 9.0f, Component.literal("the dry lance's speed on stone"));

        // Sneaking, a charged lance mines a single block and pays nothing for it.
        cube(helper);
        var sneaking = charged(CHARGED);
        var sneaker = miner(helper, sneaking);
        sneaker.setShiftKeyDown(true);
        mine(helper, sneaker, CENTRE);
        helper.assertBlockPresent(Blocks.STONE, CENTRE.south());
        helper.assertValueEqual(BoreLance.energy(sneaking), CHARGED, Component.literal("FE a sneaking mine cost"));
        helper.succeed();
    }

    private static void lunges(GameTestHelper helper) {
        var lunge = enchantment(helper, Enchantments.LUNGE);
        helper.assertValueEqual(GearContent.NETHERITE_BORE_LANCE.get().lunge(), 3, Component.literal("the netherite lance's Lunge"));
        var lance = charged(CHARGED);
        helper.assertValueEqual(lance.getEnchantmentLevel(lunge), 3, Component.literal("a charged netherite lance's Lunge"));
        helper.assertFalse(lance.supportsEnchantment(lunge), Component.literal("the netherite lance takes a Lunge book"));

        double netherite = lunge(helper, lance, 20);
        double diamond = lunge(helper, new ItemStack(GearContent.DIAMOND_BORE_LANCE.get()), 20);

        helper.assertTrue(netherite > diamond * 1.2, Component.literal("Lunge III carried the player " + netherite + ", no further than Lunge II's " + diamond));
        helper.assertValueEqual(BoreLance.energy(lance), CHARGED - 3 * GearworksConfig.netheriteLanceLungeEnergy(),
                Component.literal("the lance's FE after a lunge"));
        helper.succeed();
    }

    private static void noFoodGate(GameTestHelper helper) {
        var lance = charged(CHARGED);
        var player = northOfCube(helper, LanceTests.holding(helper, lance));
        player.getFoodData().setFoodLevel(2);

        jab(lance, player);

        helper.assertTrue(player.getDeltaMovement().horizontalDistance() > 0.3,
                Component.literal("a player with 2 food lunged " + player.getDeltaMovement().horizontalDistance()));
        helper.assertValueEqual(player.getFoodData().getFoodLevel(), 2, Component.literal("the player's food after the lunge"));
        helper.succeed();
    }

    private static void dryNoLunge(GameTestHelper helper) {
        var lunge = enchantment(helper, Enchantments.LUNGE);
        // A lance one FE short of a lunge's cost is dry for Lunge.
        var lance = charged(3 * GearworksConfig.netheriteLanceLungeEnergy() - 1);
        helper.assertValueEqual(lance.getEnchantmentLevel(lunge), 0, Component.literal("a dry netherite lance's Lunge"));

        helper.assertValueEqual(lunge(helper, lance, 20), 0.0, Component.literal("a dry netherite lance's lunge"));
        helper.assertValueEqual(BoreLance.energy(lance), 3 * GearworksConfig.netheriteLanceLungeEnergy() - 1, Component.literal("a dry lance's FE after a jab"));
        helper.succeed();
    }

    private static void lungeBoring(GameTestHelper helper) {
        wall(helper);
        var lance = charged(CHARGED);
        var player = LanceTests.holding(helper, lance);
        var feet = helper.absoluteVec(new Vec3(4.5, 1, CENTRE.getZ() - 0.5));
        player.setPos(feet.x, feet.y, feet.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition().add(0, 0, 10));

        jab(lance, player);

        // The netherite lance's 5×3 Bore, centred on the block ahead of the player's head and three deep.
        var bore = new ArrayList<BlockPos>();
        for (int x = CENTRE.getX() - 2; x <= CENTRE.getX() + 2; x++) {
            for (int y = CENTRE.getY() - 1; y <= CENTRE.getY() + 1; y++) {
                for (int z = CENTRE.getZ(); z < CENTRE.getZ() + 3; z++) {
                    bore.add(new BlockPos(x, y, z));
                }
            }
        }
        assertBroken(helper, bore);
        helper.assertValueEqual(BoreLance.energy(lance),
                CHARGED - 3 * GearworksConfig.netheriteLanceLungeEnergy() - 45 * GearworksConfig.netheriteLanceLungeBoringEnergy(),
                Component.literal("the lance's FE after a lunge and 45 blocks"));

        // With FE for the lunge and two blocks, it bores the player's way two blocks and stops.
        wall(helper);
        var short_ = charged(3 * GearworksConfig.netheriteLanceLungeEnergy() + 2 * GearworksConfig.netheriteLanceLungeBoringEnergy());
        var lancer = LanceTests.holding(helper, short_);
        lancer.setPos(feet.x, feet.y, feet.z);
        lancer.lookAt(EntityAnchorArgument.Anchor.EYES, lancer.getEyePosition().add(0, 0, 10));
        jab(short_, lancer);
        int broken = 0;
        for (var block : bore) {
            broken += helper.getBlockState(block).isAir() ? 1 : 0;
        }
        helper.assertValueEqual(broken, 2, Component.literal("blocks a lance with FE for two broke"));
        helper.assertValueEqual(BoreLance.energy(short_), 0, Component.literal("the short lance's FE"));
        helper.succeed();
    }

    private static void suitCharges(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get());
        int capacity = Suit.capacity(suit);
        charge(suit, capacity);
        var player = wearer(helper, suit);
        var lance = charged(0);
        int rate = GearworksConfig.netheriteLanceChargeRate();

        player.setItemSlot(EquipmentSlot.MAINHAND, lance);
        player.tick();
        helper.assertValueEqual(BoreLance.energy(lance), rate, Component.literal("the lance's FE after a tick in the main hand"));

        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.OFFHAND, lance);
        player.tick();
        helper.assertValueEqual(BoreLance.energy(lance), 2 * rate, Component.literal("the lance's FE after a tick in the off hand"));
        helper.assertValueEqual(Suit.energy(suit), capacity - 2 * rate, Component.literal("the suit's FE after charging the lance"));

        // In the inventory, it charges nothing.
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        player.getInventory().setItem(10, lance);
        player.tick();
        helper.assertValueEqual(BoreLance.energy(lance), 2 * rate, Component.literal("the lance's FE after a tick in the inventory"));
        helper.succeed();
    }

    private static void suitKeepsItsReserve(GameTestHelper helper) {
        var suit = suitWith(GearContent.BATTERY_UPGRADE.get());
        int reserve = LanceCharge.kept(Suit.capacity(suit), GearworksConfig.netheriteLanceChargeReserve());
        charge(suit, reserve + 300);
        var player = wearer(helper, suit);
        var lance = charged(0);
        player.setItemSlot(EquipmentSlot.MAINHAND, lance);

        player.tick();
        helper.assertValueEqual(BoreLance.energy(lance), 300, Component.literal("the lance's FE from a suit 300 over its reserve"));
        helper.assertValueEqual(Suit.energy(suit), reserve, Component.literal("the suit's FE"));

        player.tick();
        helper.assertValueEqual(BoreLance.energy(lance), 300, Component.literal("the lance's FE from a suit at its reserve"));
        helper.succeed();
    }

    /** A netherite lance holding that much FE, put in through its capability as a charger would. */
    private static ItemStack charged(int fe) {
        var lance = new ItemStack(GearContent.NETHERITE_BORE_LANCE.get());
        if (fe > 0) {
            try (var transaction = Transaction.openRoot()) {
                ItemAccess.forStack(lance).getCapability(Capabilities.Energy.ITEM).insert(fe, transaction);
                transaction.commit();
            }
        }
        return lance;
    }

    /** A cube of stone five across, three high and three deep, its near face at z = 3: the netherite tier's Bore. */
    private static void cube(GameTestHelper helper) {
        for (var pos : cubeBlocks()) {
            helper.setBlock(pos, Blocks.STONE);
        }
    }

    private static List<BlockPos> cubeBlocks() {
        var blocks = new ArrayList<BlockPos>();
        for (int x = 2; x <= 6; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = CENTRE.getZ(); z < CENTRE.getZ() + 3; z++) {
                    blocks.add(new BlockPos(x, y, z));
                }
            }
        }
        return blocks;
    }

    /** A wall of stone across the platform, from z = 3 to its far edge, as high as the platform's air. */
    private static void wall(GameTestHelper helper) {
        for (int x = 1; x <= 7; x++) {
            for (int y = 1; y <= 4; y++) {
                for (int z = CENTRE.getZ(); z <= 8; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
    }

    private static ServerPlayer miner(GameTestHelper helper, ItemStack lance) {
        var player = northOfCube(helper, LanceTests.holding(helper, lance));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(new Vec3(4.5, 2.5, CENTRE.getZ())));
        return player;
    }

    private static ServerPlayer northOfCube(GameTestHelper helper, ServerPlayer player) {
        var feet = helper.absoluteVec(new Vec3(4.5, 1, 1.5));
        player.setPos(feet.x, feet.y, feet.z);
        return player;
    }

    private static void mine(GameTestHelper helper, ServerPlayer player, BlockPos block) {
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(block)), Component.literal("the player could not break " + block.toShortString()));
    }

    private static void jab(ItemStack lance, ServerPlayer player) {
        lance.get(DataComponents.PIERCING_WEAPON).attack(player, EquipmentSlot.MAINHAND);
    }

    /** How far forward a jab with the lance carries a player with that much food, where nothing stands in front. */
    private static double lunge(GameTestHelper helper, ItemStack lance, int food) {
        var player = northOfCube(helper, LanceTests.holding(helper, lance));
        player.getFoodData().setFoodLevel(food);
        jab(lance, player);
        return player.getDeltaMovement().horizontalDistance();
    }

    private static void assertBroken(GameTestHelper helper, List<BlockPos> blocks) {
        for (var block : blocks) {
            helper.assertBlockPresent(Blocks.AIR, block);
        }
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static void chargingKeepsTheHand(GameTestHelper helper) {
        var before = charged(CHARGED);
        var after = charged(CHARGED + 1_000);
        var enchanted = charged(CHARGED);
        enchanted.enchant(enchantment(helper, Enchantments.FORTUNE), 3);

        helper.assertFalse(before.shouldCauseBlockBreakReset(after), Component.literal("charging the lance resets its mining"));
        helper.assertFalse(before.getItem().shouldCauseReequipAnimation(before, after, false),
                Component.literal("charging the lance reequips it"));
        helper.assertTrue(before.shouldCauseBlockBreakReset(enchanted), Component.literal("enchanting the lance doesn't reset its mining"));
        helper.assertTrue(before.getItem().shouldCauseReequipAnimation(before, after, true),
                Component.literal("switching to another lance doesn't reequip it"));
        helper.succeed();
    }
}
