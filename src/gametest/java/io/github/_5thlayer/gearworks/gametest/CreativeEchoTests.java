// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import static io.github._5thlayer.gearworks.gametest.SuitFixtures.charge;
import static io.github._5thlayer.gearworks.gametest.SuitFixtures.suitWith;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.lance.BoreLance;
import io.github._5thlayer.gearworks.railgun.Railgun;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The creative inventory sends back every slot the server changes, a tick or more late, and the server
 * sets what it gets. A Suit, Bore Lance or Railgun changes its FE or Charge every tick, so the copy that
 * comes back differs from the slot's only in that: the server keeps its own, and the Suit is not put on
 * again, so its equip sound doesn't play.
 */
final class CreativeEchoTests {

    private static final int CHEST_SLOT = InventoryMenu.ARMOR_SLOT_START + 1;

    /** The hotbar's first slot, which a new player holds. */
    private static final int MAIN_HAND_SLOT = InventoryMenu.USE_ROW_SLOT_START;

    private CreativeEchoTests() {
    }

    static void register(GearworksGameTests.Registrar tests) {
        tests.test("creative_echo_of_a_charging_suit_keeps_its_fe_and_plays_no_equip_sound", 1, CreativeEchoTests::suit);
        tests.test("creative_echo_of_a_charging_bore_lance_keeps_its_fe", 1, CreativeEchoTests::lance);
        tests.test("creative_echo_of_a_charging_railgun_keeps_its_charge_and_fe", 1, CreativeEchoTests::railgun);
        tests.test("creative_echo_a_creative_slot_still_takes_another_stack", 1, CreativeEchoTests::otherStacks);
        tests.test("creative_echo_a_suit_put_on_from_the_creative_inventory_plays_its_equip_sound_once", 1, CreativeEchoTests::putOn);
    }

    private static void suit(GameTestHelper helper) {
        var player = creativePlayer(helper);
        try {
            var suit = suitWith(GearContent.BATTERY_UPGRADE.get());
            player.setItemSlot(EquipmentSlot.CHEST, suit);
            var echoed = suit.copy();
            charge(suit, 100);
            long heard = HeardSounds.count(helper, equipSound(suit));

            echo(player, CHEST_SLOT, echoed);

            var worn = player.getItemBySlot(EquipmentSlot.CHEST);
            helper.assertValueEqual(Suit.energy(worn), 100, Component.literal("the worn Suit's FE after its echo"));
            helper.assertValueEqual(HeardSounds.count(helper, equipSound(suit)) - heard, 0L, Component.literal("equip sounds the echo played"));
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    private static void lance(GameTestHelper helper) {
        var player = creativePlayer(helper);
        try {
            var lance = new ItemStack(GearContent.NETHERITE_BORE_LANCE.get());
            BoreLance.fill(lance, 100);
            player.setItemSlot(EquipmentSlot.MAINHAND, lance);
            var echoed = lance.copy();
            BoreLance.fill(lance, 100);

            echo(player, MAIN_HAND_SLOT, echoed);

            helper.assertValueEqual(BoreLance.energy(player.getMainHandItem()), 200, Component.literal("the held lance's FE after its echo"));
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    private static void railgun(GameTestHelper helper) {
        var player = creativePlayer(helper);
        try {
            var railgun = new ItemStack(GearContent.RAILGUN.get());
            fill(railgun, 50_000);
            player.setItemSlot(EquipmentSlot.MAINHAND, railgun);
            int ticks = GearworksConfig.railgunChargeTicks();
            tick(player, ticks / 2);
            var echoed = railgun.copy();
            tick(player, ticks - ticks / 2);
            helper.assertTrue(Railgun.charged(railgun), Component.literal("the Railgun is not Charged after its Charge time"));

            echo(player, MAIN_HAND_SLOT, echoed);

            var held = player.getMainHandItem();
            helper.assertTrue(Railgun.charged(held), Component.literal("the held Railgun is not Charged after its echo"));
            helper.assertValueEqual(Railgun.chargeLeft(helper.getLevel(), held), 0f, Component.literal("the held Railgun's Charge left after its echo"));
            helper.assertValueEqual(Railgun.energy(held), 50_000 - GearworksConfig.railgunChargeEnergy(),
                    Component.literal("the held Railgun's FE after its echo"));
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    private static void otherStacks(GameTestHelper helper) {
        var player = creativePlayer(helper);
        try {
            var suit = suitWith(GearContent.BATTERY_UPGRADE.get());
            charge(suit, 100);
            player.setItemSlot(EquipmentSlot.CHEST, suit);

            // Another grid is another Suit, whatever its FE.
            var regridded = suitWith(GearContent.BATTERY_UPGRADE.get(), GearContent.SOLAR_UPGRADE.get());
            echo(player, CHEST_SLOT, regridded);
            var worn = player.getItemBySlot(EquipmentSlot.CHEST);
            helper.assertValueEqual(Suit.energy(worn), 0, Component.literal("the FE of a Suit with another grid set over a charged one"));
            helper.assertTrue(ItemStack.isSameItemSameComponents(worn, regridded), Component.literal("the Suit with another grid was not set"));

            echo(player, CHEST_SLOT, new ItemStack(Items.IRON_CHESTPLATE));
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), Component.literal("a chestplate was not set over the Suit"));

            echo(player, CHEST_SLOT, ItemStack.EMPTY);
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), Component.literal("the chest slot was not emptied"));
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    private static void putOn(GameTestHelper helper) {
        var player = creativePlayer(helper);
        try {
            var suit = suitWith(GearContent.BATTERY_UPGRADE.get());
            long heard = HeardSounds.count(helper, equipSound(suit));

            echo(player, CHEST_SLOT, suit);

            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).is(suit.getItem()), Component.literal("the Suit was not put on"));
            helper.assertValueEqual(HeardSounds.count(helper, equipSound(suit)) - heard, 1L, Component.literal("equip sounds putting the Suit on played"));
        } finally {
            leave(player);
        }
        helper.succeed();
    }

    private static void fill(ItemStack stack, int fe) {
        try (var transaction = Transaction.openRoot()) {
            ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM).insert(fe, transaction);
            transaction.commit();
        }
    }

    private static void tick(ServerPlayer player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            player.doTick();
        }
    }

    private static SoundEvent equipSound(ItemStack suit) {
        return suit.get(DataComponents.EQUIPPABLE).equipSound().value();
    }

    /** That stack arriving from the creative inventory for that slot of the player's inventory menu. */
    private static void echo(ServerPlayer player, int slot, ItemStack stack) {
        player.connection.handleSetCreativeModeSlot(new ServerboundSetCreativeModeSlotPacket(slot, stack));
    }

    /**
     * A creative player on the server, in the platform, with a real packet listener: NeoForge's fake
     * player drops creative slot packets. Ticked once, since nothing counts as put on in an entity's
     * first tick.
     */
    private static ServerPlayer creativePlayer(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "gearworks_creative"), false);
        var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.CREATIVE);
        var feet = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
        player.teleportTo(feet.x, feet.y, feet.z);
        tick(player, 1);
        return player;
    }

    private static void leave(ServerPlayer player) {
        player.level().getServer().getPlayerList().remove(player);
    }
}
