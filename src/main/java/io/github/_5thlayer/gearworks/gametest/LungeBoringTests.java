// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.lance.BoreLance;
import io.github._5thlayer.gearworks.lance.LungeBoring;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Lunge Boring: a jab whose Lunge carries the player forward breaks the lance's Bore through the rock
 * ahead, and goes on breaking it ahead of them while the lunge lasts, at {@link GearworksConfig#lungeBoringCost()}
 * durability a block. A player stands just north of a wall of stone, facing south into it; its near face
 * is at z = 3. The test player moves only where a test puts it, so a test moves it as Lunge would and
 * ticks the dash itself.
 */
final class LungeBoringTests {

    /** The block of the wall at the player's head height, straight ahead of them. */
    private static final BlockPos HEAD = new BlockPos(4, 2, 3);

    private LungeBoringTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("lunge_boring_breaks_the_bore_ahead_while_lunge_carries_the_player", 1, LungeBoringTests::boresAhead);
        tests.test("lunge_boring_always_opens_the_players_way", 1, LungeBoringTests::opensTheWay);
        tests.test("the_dash_stops_at_the_first_block_the_lance_cannot_break", 1, LungeBoringTests::stops);
        tests.test("lunge_boring_leaves_blocks_off_the_players_way_it_cannot_break", 1, LungeBoringTests::leaves);
        tests.test("lunge_boring_breaks_harder_blocks_a_mined_bore_would_skip", 1, LungeBoringTests::harder);
        tests.test("a_lance_that_wears_out_mid_dash_stops_it", 1, LungeBoringTests::wearsOut);
        tests.test("a_jab_that_does_not_lunge_bores_nothing", 1, LungeBoringTests::noLunge);
    }

    private static void boresAhead(GameTestHelper helper) {
        wall(helper);
        var lance = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        var player = lancer(helper, lance);

        jab(lance, player);

        // The diamond lance's 3×3 Bore, centred on the block ahead of the player's head and three deep.
        assertBroken(helper, bore(3, 5));
        assertStanding(helper, layer(6));
        helper.assertValueEqual(lance.getDamageValue(), 1 + 27 * GearworksConfig.lungeBoringCost(),
                Component.literal("the lance's wear after a lunge and 27 blocks"));

        // Carried a block on, it breaks the next layer.
        step(helper, player, 1);
        LungeBoring.tick(player);
        assertBroken(helper, bore(6, 6));
        assertStanding(helper, layer(7));

        // Lunge II carries two blocks: past that, the dash bores no further on.
        step(helper, player, 3);
        LungeBoring.tick(player);
        assertBroken(helper, bore(7, 7));
        helper.assertBlockPresent(Blocks.STONE, HEAD.south(5));

        // The lunge spent, a wall rebuilt in front of the player stands.
        for (int tick = 0; tick < LungeBoring.DASH_TICKS; tick++) {
            LungeBoring.tick(player);
        }
        wall(helper);
        LungeBoring.tick(player);
        assertStanding(helper, bore(3, 8));
        helper.succeed();
    }

    private static void opensTheWay(GameTestHelper helper) {
        wall(helper);
        var lance = new ItemStack(GearContent.IRON_BORE_LANCE.get());
        var player = lancer(helper, lance);

        jab(lance, player);

        // The iron lance's Bore is 1×1, at head height; the block below it is the player's way too.
        var way = new ArrayList<BlockPos>();
        for (int in = 0; in < 3; in++) {
            way.addAll(List.of(HEAD.south(in), HEAD.below().south(in)));
        }
        assertBroken(helper, way);
        helper.assertBlockPresent(Blocks.STONE, HEAD.above());
        helper.assertBlockPresent(Blocks.STONE, HEAD.west());
        helper.assertValueEqual(lance.getDamageValue(), 1 + 6 * GearworksConfig.lungeBoringCost(),
                Component.literal("the lance's wear after a lunge and 6 blocks"));
        helper.succeed();
    }

    private static void stops(GameTestHelper helper) {
        wall(helper);
        // The iron lance can't mine obsidian, in the player's way a block in.
        var obsidian = HEAD.below().south();
        helper.setBlock(obsidian, Blocks.OBSIDIAN);
        var lance = new ItemStack(GearContent.IRON_BORE_LANCE.get());
        var player = lancer(helper, lance);

        jab(lance, player);

        assertBroken(helper, List.of(HEAD, HEAD.below()));
        helper.assertBlockPresent(Blocks.OBSIDIAN, obsidian);
        // Nothing past it, not even the rest of its layer.
        helper.assertBlockPresent(Blocks.STONE, HEAD.south());
        helper.assertBlockPresent(Blocks.STONE, HEAD.south(2));

        // The dash is over: carried on, it breaks nothing more.
        step(helper, player, 1);
        LungeBoring.tick(player);
        helper.assertBlockPresent(Blocks.STONE, HEAD.south());
        helper.succeed();
    }

    private static void leaves(GameTestHelper helper) {
        wall(helper);
        var dirt = HEAD.above().west();
        helper.setBlock(dirt, Blocks.DIRT);
        var lance = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        var player = lancer(helper, lance);

        jab(lance, player);

        helper.assertBlockPresent(Blocks.DIRT, dirt);
        var rest = new ArrayList<>(bore(3, 5));
        rest.remove(dirt);
        assertBroken(helper, rest);
        helper.succeed();
    }

    private static void harder(GameTestHelper helper) {
        wall(helper);
        // Mining stone, a Bore skips these: a lunge has no hit block to measure them against.
        var deepslate = HEAD.above().west();
        var obsidian = HEAD.below().south();
        helper.setBlock(deepslate, Blocks.DEEPSLATE);
        helper.setBlock(obsidian, Blocks.OBSIDIAN);
        var lance = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        var player = lancer(helper, lance);

        jab(lance, player);

        assertBroken(helper, bore(3, 5));
        helper.succeed();
    }

    private static void wearsOut(GameTestHelper helper) {
        wall(helper);
        var lance = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        int cost = GearworksConfig.lungeBoringCost();
        // Enough for the lunge, two blocks, and a last block that wears it out.
        lance.setDamageValue(lance.getMaxDamage() - 1 - (1 + 2 * cost + 1));
        var player = lancer(helper, lance);

        jab(lance, player);

        int broken = 0;
        for (var block : bore(3, 5)) {
            broken += helper.getBlockState(block).isAir() ? 1 : 0;
        }
        helper.assertValueEqual(broken, 3, Component.literal("blocks a lance with three blocks' wear left broke"));
        helper.assertTrue(BoreLance.wornOut(lance), Component.literal("the lance is not worn out"));
        helper.succeed();
    }

    private static void noLunge(GameTestHelper helper) {
        wall(helper);
        var lance = new ItemStack(GearContent.DIAMOND_BORE_LANCE.get());
        var player = lancer(helper, lance);
        player.getFoodData().setFoodLevel(6);

        jab(lance, player);
        LungeBoring.tick(player);

        assertStanding(helper, bore(3, 5));
        helper.succeed();
    }

    /** A wall of stone across the platform, from z = 3 to its far edge, as high as the platform's air. */
    private static void wall(GameTestHelper helper) {
        for (int x = 1; x <= 7; x++) {
            for (int y = 1; y <= 4; y++) {
                for (int z = HEAD.getZ(); z <= 8; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
    }

    /** A survival player standing against the wall's face, holding the lance and looking straight into it. */
    private static ServerPlayer lancer(GameTestHelper helper, ItemStack lance) {
        var player = LanceTests.holding(helper, lance);
        step(helper, player, 0);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition().add(0, 0, 10));
        return player;
    }

    /** Puts the player that many blocks into the wall from where they stood against it. */
    private static void step(GameTestHelper helper, ServerPlayer player, int blocks) {
        var feet = helper.absoluteVec(new Vec3(4.5, 1, HEAD.getZ() - 0.5 + blocks));
        player.setPos(feet.x, feet.y, feet.z);
    }

    private static void jab(ItemStack lance, ServerPlayer player) {
        lance.get(DataComponents.PIERCING_WEAPON).attack(player, EquipmentSlot.MAINHAND);
    }

    /** The diamond lance's 3×3 Bore around the column ahead of the player's head, between those z. */
    private static List<BlockPos> bore(int fromZ, int toZ) {
        var blocks = new ArrayList<BlockPos>();
        for (int x = HEAD.getX() - 1; x <= HEAD.getX() + 1; x++) {
            for (int y = HEAD.getY() - 1; y <= HEAD.getY() + 1; y++) {
                for (int z = fromZ; z <= toZ; z++) {
                    blocks.add(new BlockPos(x, y, z));
                }
            }
        }
        return blocks;
    }

    /** The 3×3 around the column ahead of the player's head, at that z. */
    private static List<BlockPos> layer(int z) {
        return bore(z, z);
    }

    private static void assertBroken(GameTestHelper helper, List<BlockPos> blocks) {
        for (var block : blocks) {
            helper.assertBlockPresent(Blocks.AIR, block);
        }
    }

    private static void assertStanding(GameTestHelper helper, List<BlockPos> blocks) {
        for (var block : blocks) {
            helper.assertBlockPresent(Blocks.STONE, block);
        }
    }
}
