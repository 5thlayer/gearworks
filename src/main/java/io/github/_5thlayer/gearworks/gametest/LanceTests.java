// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.lance.Bore;
import io.github._5thlayer.gearworks.lance.BoreLance;
import io.github._5thlayer.gearworks.lance.BoreBreaker;
import io.github._5thlayer.gearworks.lance.CycleBorePayload;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * The iron and diamond Bore Lance: each its material's spear and a little more, with Lunge built in at
 * its tier's level, made from the spear and from the tier below keeping their enchantments, mining up
 * to its material's level, and breaking its Bore on the face hit, three deep, each block as the player
 * breaking it. A player looks at a cube of rock and breaks one block of it as the server does when
 * mining ends; the cube is three by three by three, so it is exactly the diamond tier's Bore.
 */
final class LanceTests {

    /** The cube's near face is at z = 3, its centre block at (4, 2, 3); the player stands north of it. */
    private static final BlockPos CENTRE = new BlockPos(4, 2, 3);

    private LanceTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("each_lance_hits_harder_than_its_materials_spear", 1, LanceTests::hitsHarder);
        tests.test("each_lance_has_lunge_built_in_at_its_tiers_level", 1, LanceTests::builtInLunge);
        tests.test("a_lances_lunge_carries_the_player_forward", 1, LanceTests::lunges);
        tests.test("a_lance_lunges_only_with_food_above_six", 1, LanceTests::lungeNeedsFood);
        tests.test("crafting_the_iron_lance_from_an_iron_spear_keeps_its_enchantments", 1, LanceTests::craftsIron);
        tests.test("crafting_the_diamond_lance_from_the_iron_lance_keeps_its_enchantments", 1, LanceTests::craftsDiamond);
        tests.test("each_lance_mines_up_to_its_materials_level", 1, LanceTests::minesToItsLevel);
        tests.test("the_iron_lance_bores_one_by_one_by_three", 1, LanceTests::ironBores);
        tests.test("the_diamond_lance_bores_three_by_three_by_three", 1, LanceTests::diamondBores);
        tests.test("each_block_a_bore_breaks_drops_where_it_stood", 1, LanceTests::dropsWhereItStood);
        tests.test("silk_touch_applies_to_every_block_of_a_bore", 1, LanceTests::silkTouch);
        tests.test("fortune_applies_to_every_block_of_a_bore", 1, LanceTests::fortune);
        tests.test("a_bore_skips_harder_blocks_and_ones_the_lance_does_not_mine", 1, LanceTests::skips);
        tests.test("a_block_the_lance_does_not_mine_breaks_alone", 1, LanceTests::noBoreFromDirt);
        tests.test("sneaking_breaks_one_block", 1, LanceTests::sneaking);
        tests.test("a_bore_on_a_floor_lies_across_the_players_path", 1, LanceTests::floorBore);
        tests.test("the_bore_key_steps_through_the_tiers_bores_widest_first", 1, LanceTests::cycles);
        tests.test("the_iron_lance_breaks", 1, LanceTests::ironBreaks);
        tests.test("the_diamond_lance_never_breaks", 1, LanceTests::diamondNeverBreaks);
        tests.test("a_worn_out_diamond_lance_neither_bores_nor_lunges", 1, LanceTests::wornOut);
        tests.test("a_lance_that_breaks_partway_stops_its_bore", 1, LanceTests::breaksPartway);
    }

    private static void hitsHarder(GameTestHelper helper) {
        hitsHarder(helper, GearContent.IRON_BORE_LANCE.get(), Items.IRON_SPEAR);
        hitsHarder(helper, GearContent.DIAMOND_BORE_LANCE.get(), Items.DIAMOND_SPEAR);
        helper.succeed();
    }

    private static void hitsHarder(GameTestHelper helper, Item lance, Item spear) {
        helper.assertTrue(held(lance, Attributes.ATTACK_DAMAGE) > held(spear, Attributes.ATTACK_DAMAGE),
                Component.literal(lance + " hits no harder than " + spear));
        helper.assertValueEqual(held(lance, Attributes.ATTACK_SPEED), held(spear, Attributes.ATTACK_SPEED),
                Component.literal(lance + "'s attack speed"));
        helper.assertTrue(new ItemStack(lance).get(DataComponents.KINETIC_WEAPON).damageMultiplier()
                        > new ItemStack(spear).get(DataComponents.KINETIC_WEAPON).damageMultiplier(),
                Component.literal(lance + "'s charge hits no harder than " + spear + "'s"));
        helper.assertTrue(new ItemStack(lance).getMaxDamage() > new ItemStack(spear).getMaxDamage(),
                Component.literal(lance + " wears out no slower than " + spear));
    }

    /** The attribute of a player holding that item in the main hand, from its base of 1. */
    private static double held(Item item, Holder<Attribute> attribute) {
        return new ItemStack(item).getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .compute(attribute, 1.0, EquipmentSlot.MAINHAND);
    }

    private static void builtInLunge(GameTestHelper helper) {
        var lunge = enchantment(helper, Enchantments.LUNGE);
        for (var tier : List.of(GearContent.IRON_BORE_LANCE.get(), GearContent.DIAMOND_BORE_LANCE.get())) {
            var lance = new ItemStack(tier);
            helper.assertValueEqual(lance.getEnchantmentLevel(lunge), tier.lunge(), Component.literal(tier + "'s Lunge"));
            helper.assertValueEqual(lance.getAllEnchantments(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)).getLevel(lunge),
                    tier.lunge(), Component.literal(tier + "'s Lunge among its enchantments"));
            // On no enchantment the grindstone takes off, and no book raises it.
            helper.assertFalse(lance.isEnchanted(), Component.literal(tier + " carries its Lunge as an enchantment a grindstone removes"));
            helper.assertFalse(lance.supportsEnchantment(lunge), Component.literal(tier + " takes a Lunge book"));
        }
        helper.assertValueEqual(GearContent.IRON_BORE_LANCE.get().lunge(), 1, Component.literal("the iron lance's Lunge"));
        helper.assertValueEqual(GearContent.DIAMOND_BORE_LANCE.get().lunge(), 2, Component.literal("the diamond lance's Lunge"));
        helper.succeed();
    }

    private static void lunges(GameTestHelper helper) {
        double iron = lunge(helper, new ItemStack(GearContent.IRON_BORE_LANCE.get()), 20);
        double diamond = lunge(helper, new ItemStack(GearContent.DIAMOND_BORE_LANCE.get()), 20);
        helper.assertTrue(iron > 0.3, Component.literal("the iron lance's jab carried the player " + iron));
        helper.assertTrue(diamond > iron * 1.5, Component.literal("the diamond lance's Lunge II carried the player no further than Lunge I"));
        helper.succeed();
    }

    private static void lungeNeedsFood(GameTestHelper helper) {
        helper.assertValueEqual(lunge(helper, new ItemStack(GearContent.DIAMOND_BORE_LANCE.get()), 6), 0.0,
                Component.literal("the lunge of a player with 6 food"));
        helper.succeed();
    }

    private static void craftsIron(GameTestHelper helper) {
        var spear = new ItemStack(Items.IRON_SPEAR);
        spear.enchant(enchantment(helper, Enchantments.SHARPNESS), 2);
        spear.enchant(enchantment(helper, Enchantments.LUNGE), 3);
        spear.setDamageValue(10);
        var copper = new ItemStack(Items.COPPER_INGOT);
        var redstone = new ItemStack(Items.REDSTONE);
        var input = CraftingInput.of(3, 3, List.of(copper, redstone, copper, copper, spear, copper, copper, redstone, copper));
        var level = helper.getLevel();
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        helper.assertTrue(recipe.isPresent(), Component.literal("no recipe makes the iron lance from an iron spear"));

        var lance = recipe.get().value().assemble(input);

        helper.assertTrue(lance.is(GearContent.IRON_BORE_LANCE.get()), Component.literal("the recipe makes " + lance));
        helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(enchantment(helper, Enchantments.SHARPNESS), lance), 2,
                Component.literal("the lance's Sharpness"));
        helper.assertValueEqual(lance.getDamageValue(), 10, Component.literal("the lance's wear"));
        // The spear's Lunge III gives way to the lance's own.
        helper.assertValueEqual(lance.getEnchantmentLevel(enchantment(helper, Enchantments.LUNGE)), 1, Component.literal("the lance's Lunge"));
        helper.assertFalse(lance.getTagEnchantments().keySet().contains(enchantment(helper, Enchantments.LUNGE)),
                Component.literal("the spear's Lunge stayed on the lance"));
        helper.succeed();
    }

    private static void craftsDiamond(GameTestHelper helper) {
        var iron = new ItemStack(GearContent.IRON_BORE_LANCE.get());
        iron.enchant(enchantment(helper, Enchantments.FORTUNE), 3);
        var items = new ArrayList<ItemStack>(List.of(iron, new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND)));
        var input = CraftingInput.of(2, 2, items);
        var level = helper.getLevel();
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
        helper.assertTrue(recipe.isPresent(), Component.literal("no recipe makes the diamond lance from the iron lance and three diamonds"));

        var diamond = recipe.get().value().assemble(input);

        helper.assertTrue(diamond.is(GearContent.DIAMOND_BORE_LANCE.get()), Component.literal("the recipe makes " + diamond));
        helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(enchantment(helper, Enchantments.FORTUNE), diamond), 3,
                Component.literal("the diamond lance's Fortune"));
        helper.succeed();
    }

    private static void minesToItsLevel(GameTestHelper helper) {
        var iron = new ItemStack(GearContent.IRON_BORE_LANCE.get());
        var diamond = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        helper.assertTrue(iron.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), Component.literal("the iron lance can't mine diamond ore"));
        helper.assertFalse(iron.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), Component.literal("the iron lance mines obsidian"));
        helper.assertTrue(diamond.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), Component.literal("the diamond lance can't mine obsidian"));
        helper.assertValueEqual(iron.getDestroySpeed(Blocks.STONE.defaultBlockState()), 6.0f, Component.literal("the iron lance's speed on stone"));
        helper.assertValueEqual(diamond.getDestroySpeed(Blocks.STONE.defaultBlockState()), 8.0f, Component.literal("the diamond lance's speed on stone"));
        helper.assertFalse(iron.isCorrectToolForDrops(Blocks.DIRT.defaultBlockState()), Component.literal("the iron lance mines dirt as a shovel"));
        helper.succeed();
    }

    private static void ironBores(GameTestHelper helper) {
        cube(helper, Blocks.STONE);
        var lance = new ItemStack(GearContent.IRON_BORE_LANCE.get());
        var player = miner(helper, lance);

        mine(helper, player, CENTRE);

        var column = List.of(CENTRE, CENTRE.south(), CENTRE.south(2));
        assertBroken(helper, column);
        helper.assertValueEqual(lance.getDamageValue(), 3, Component.literal("the lance's wear after three blocks"));
        helper.succeed();
    }

    private static void diamondBores(GameTestHelper helper) {
        cube(helper, Blocks.STONE);
        var lance = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        var player = miner(helper, lance);

        mine(helper, player, CENTRE);

        assertBroken(helper, cubeBlocks());
        helper.assertValueEqual(lance.getDamageValue(), 27, Component.literal("the lance's wear after 27 blocks"));
        helper.succeed();
    }

    private static void dropsWhereItStood(GameTestHelper helper) {
        cube(helper, Blocks.STONE);
        var player = miner(helper, new ItemStack(GearContent.DIAMOND_BORE_LANCE.get()));

        mine(helper, player, CENTRE);

        for (var block : cubeBlocks()) {
            var drops = drops(helper, block);
            helper.assertValueEqual(drops.size(), 1, Component.literal("item entities where " + block.toShortString() + " stood"));
            helper.assertTrue(drops.getFirst().getItem().is(Items.COBBLESTONE), Component.literal(block.toShortString() + " dropped " + drops.getFirst().getItem()));
        }
        helper.succeed();
    }

    private static void silkTouch(GameTestHelper helper) {
        cube(helper, Blocks.STONE);
        var lance = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        lance.enchant(enchantment(helper, Enchantments.SILK_TOUCH), 1);
        var player = miner(helper, lance);

        mine(helper, player, CENTRE);

        for (var block : cubeBlocks()) {
            var drops = drops(helper, block);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(Items.STONE),
                    Component.literal(block.toShortString() + " dropped " + drops.stream().map(ItemEntity::getItem).toList() + ", not stone"));
        }
        helper.succeed();
    }

    private static void fortune(GameTestHelper helper) {
        cube(helper, Blocks.COAL_ORE);
        var lance = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        lance.enchant(enchantment(helper, Enchantments.FORTUNE), 3);
        var player = miner(helper, lance);

        mine(helper, player, CENTRE);

        int coal = 0;
        for (var block : cubeBlocks()) {
            for (var drop : drops(helper, block)) {
                coal += drop.getItem().is(Items.COAL) ? drop.getItem().getCount() : 0;
            }
        }
        // Without Fortune each ore drops one coal; with Fortune III, 27 ores all dropping one is (2/5)^27.
        helper.assertTrue(coal > cubeBlocks().size(), Component.literal("27 ores mined with Fortune III dropped " + coal + " coal"));
        helper.succeed();
    }

    private static void skips(GameTestHelper helper) {
        cube(helper, Blocks.STONE);
        var harder = CENTRE.above().south();
        var dirt = CENTRE.below().south(2);
        helper.setBlock(harder, Blocks.DEEPSLATE);
        helper.setBlock(dirt, Blocks.DIRT);
        var player = miner(helper, new ItemStack(GearContent.DIAMOND_BORE_LANCE.get()));

        mine(helper, player, CENTRE);

        helper.assertBlockPresent(Blocks.DEEPSLATE, harder);
        helper.assertBlockPresent(Blocks.DIRT, dirt);
        var rest = new ArrayList<>(cubeBlocks());
        rest.removeAll(List.of(harder, dirt));
        assertBroken(helper, rest);
        helper.succeed();
    }

    private static void noBoreFromDirt(GameTestHelper helper) {
        cube(helper, Blocks.STONE);
        helper.setBlock(CENTRE, Blocks.DIRT);
        var player = miner(helper, new ItemStack(GearContent.DIAMOND_BORE_LANCE.get()));

        mine(helper, player, CENTRE);

        assertOnlyBroken(helper, List.of(CENTRE));
        helper.succeed();
    }

    private static void sneaking(GameTestHelper helper) {
        cube(helper, Blocks.STONE);
        var lance = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        var player = miner(helper, lance);
        player.setShiftKeyDown(true);

        mine(helper, player, CENTRE);

        assertOnlyBroken(helper, List.of(CENTRE));
        helper.assertValueEqual(lance.getDamageValue(), 1, Component.literal("the lance's wear after one block"));
        helper.succeed();
    }

    private static void floorBore(GameTestHelper helper) {
        var hit = new BlockPos(10, 64, 10);
        var blocks = BoreBreaker.blocks(Bore.FIVE, hit, Direction.UP, Direction.NORTH);
        helper.assertValueEqual(blocks.getFirst(), hit, Component.literal("the first block of the Bore"));
        for (var block : blocks) {
            int across = block.getX() - hit.getX();
            int along = block.getZ() - hit.getZ();
            int down = hit.getY() - block.getY();
            helper.assertTrue(Math.abs(across) <= 2 && Math.abs(along) <= 1 && down >= 0 && down < Bore.DEPTH,
                    Component.literal(block.toShortString() + " lies outside a 5×3 Bore on the floor across a path north"));
        }
        helper.succeed();
    }

    private static void cycles(GameTestHelper helper) {
        var diamond = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        var player = holding(helper, diamond);
        helper.assertValueEqual(BoreLance.bore(diamond), Bore.THREE, Component.literal("a new diamond lance's Bore"));
        CycleBorePayload.cycle(player);
        helper.assertValueEqual(BoreLance.bore(diamond), Bore.ONE, Component.literal("the diamond lance's Bore after one press"));
        helper.assertValueEqual(diamond.get(GearContent.BORE.get()), Bore.ONE, Component.literal("the Bore stored on the lance"));
        CycleBorePayload.cycle(player);
        helper.assertValueEqual(BoreLance.bore(diamond), Bore.THREE, Component.literal("the diamond lance's Bore after two presses"));

        var iron = new ItemStack(GearContent.IRON_BORE_LANCE.get());
        CycleBorePayload.cycle(holding(helper, iron));
        helper.assertValueEqual(BoreLance.bore(iron), Bore.ONE, Component.literal("the iron lance's Bore after a press"));
        helper.succeed();
    }

    private static void ironBreaks(GameTestHelper helper) {
        cube(helper, Blocks.STONE);
        var lance = new ItemStack(GearContent.IRON_BORE_LANCE.get());
        lance.setDamageValue(lance.getMaxDamage() - 1);
        var player = miner(helper, lance);
        player.setShiftKeyDown(true);

        mine(helper, player, CENTRE);

        helper.assertTrue(player.getMainHandItem().isEmpty(), Component.literal("the iron lance did not break"));
        helper.succeed();
    }

    private static void diamondNeverBreaks(GameTestHelper helper) {
        var lance = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        var player = holding(helper, lance);
        lance.setDamageValue(lance.getMaxDamage() - 3);
        helper.assertFalse(BoreLance.wornOut(lance), Component.literal("the lance is worn out with three durability left"));

        lance.hurtAndBreak(10, player, EquipmentSlot.MAINHAND);

        helper.assertFalse(lance.isEmpty(), Component.literal("the diamond lance broke"));
        helper.assertValueEqual(lance.getDamageValue(), lance.getMaxDamage() - 1, Component.literal("the lance's wear"));
        helper.assertTrue(BoreLance.wornOut(lance), Component.literal("the lance is not worn out at its last durability"));
        lance.hurtAndBreak(10, player, EquipmentSlot.MAINHAND);
        helper.assertFalse(lance.isEmpty(), Component.literal("the worn-out diamond lance broke"));
        helper.succeed();
    }

    private static void wornOut(GameTestHelper helper) {
        cube(helper, Blocks.STONE);
        var lance = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        lance.setDamageValue(lance.getMaxDamage() - 1);
        helper.assertValueEqual(lance.getEnchantmentLevel(enchantment(helper, Enchantments.LUNGE)), 0, Component.literal("a worn-out lance's Lunge"));
        var player = miner(helper, lance);

        mine(helper, player, CENTRE);

        assertOnlyBroken(helper, List.of(CENTRE));
        helper.assertFalse(lance.isEmpty(), Component.literal("the worn-out lance broke mining"));

        var jabbing = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        jabbing.setDamageValue(jabbing.getMaxDamage() - 1);
        helper.assertValueEqual(lunge(helper, jabbing, 20), 0.0, Component.literal("a worn-out lance's lunge"));
        jabbing.setDamageValue(0);
        helper.assertValueEqual(jabbing.getEnchantmentLevel(enchantment(helper, Enchantments.LUNGE)), 2, Component.literal("a repaired lance's Lunge"));
        helper.succeed();
    }

    private static void breaksPartway(GameTestHelper helper) {
        cube(helper, Blocks.STONE);
        var lance = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        // Ten blocks from worn out: the hit block and nine more.
        lance.setDamageValue(lance.getMaxDamage() - 11);
        var player = miner(helper, lance);

        mine(helper, player, CENTRE);

        int broken = 0;
        for (var block : cubeBlocks()) {
            if (helper.getBlockState(block).isAir()) {
                broken++;
            }
        }
        helper.assertValueEqual(broken, 10, Component.literal("blocks broken by a lance with ten durability to spare"));
        helper.assertTrue(BoreLance.wornOut(lance), Component.literal("the lance is not worn out"));
        // Each layer before the one behind it: the far layer is untouched.
        for (var block : cubeBlocks()) {
            if (block.getZ() == CENTRE.getZ() + 2) {
                helper.assertBlockPresent(Blocks.STONE, block);
            }
        }
        helper.succeed();
    }

    /** The cube of rock, three blocks a side, its near face at z = 3. */
    private static void cube(GameTestHelper helper, Block block) {
        for (var pos : cubeBlocks()) {
            helper.setBlock(pos, block);
        }
    }

    private static List<BlockPos> cubeBlocks() {
        var blocks = new ArrayList<BlockPos>();
        for (int x = 3; x <= 5; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = CENTRE.getZ(); z < CENTRE.getZ() + 3; z++) {
                    blocks.add(new BlockPos(x, y, z));
                }
            }
        }
        return blocks;
    }

    /** A survival player north of the cube, holding the lance and looking at the middle of the cube's near face. */
    private static ServerPlayer miner(GameTestHelper helper, ItemStack lance) {
        var player = northOfCube(helper, holding(helper, lance));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(new Vec3(4.5, 2.5, CENTRE.getZ())));
        return player;
    }

    private static ServerPlayer northOfCube(GameTestHelper helper, ServerPlayer player) {
        var feet = helper.absoluteVec(new Vec3(4.5, 1, 1.5));
        player.setPos(feet.x, feet.y, feet.z);
        return player;
    }

    /**
     * A survival player holding the stack: a server player, since only one breaks blocks as the server
     * does, which the mock players of the other tests can't.
     */
    private static ServerPlayer holding(GameTestHelper helper, ItemStack stack) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "gearworks_lancer"));
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        player.setDeltaMovement(Vec3.ZERO);
        player.getFoodData().setFoodLevel(20);
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        return player;
    }

    /** Breaks the block as the server does when the player's mining of it ends. */
    private static void mine(GameTestHelper helper, ServerPlayer player, BlockPos block) {
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(block)), Component.literal("the player could not break " + block.toShortString()));
    }

    /** How far forward a jab with the lance carries a player with that much food. */
    private static double lunge(GameTestHelper helper, ItemStack lance, int food) {
        var player = northOfCube(helper, holding(helper, lance));
        player.getFoodData().setFoodLevel(food);
        lance.get(DataComponents.PIERCING_WEAPON).attack(player, EquipmentSlot.MAINHAND);
        return player.getDeltaMovement().horizontalDistance();
    }

    private static List<ItemEntity> drops(GameTestHelper helper, BlockPos block) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(block)));
    }

    private static void assertBroken(GameTestHelper helper, List<BlockPos> blocks) {
        for (var block : blocks) {
            helper.assertBlockPresent(Blocks.AIR, block);
        }
    }

    /** That those of the cube's blocks are broken, and the rest stand. */
    private static void assertOnlyBroken(GameTestHelper helper, List<BlockPos> broken) {
        for (var block : cubeBlocks()) {
            helper.assertValueEqual(helper.getBlockState(block).isAir(), broken.contains(block),
                    Component.literal("whether " + block.toShortString() + " is broken"));
        }
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
