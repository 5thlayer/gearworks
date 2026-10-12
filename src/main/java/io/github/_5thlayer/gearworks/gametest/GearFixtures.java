// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gametest;

import io.github._5thlayer.gearworks.gear.Gear;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearUpgrade;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** The suits and players the Gear tests set up. */
final class GearFixtures {

    private GearFixtures() {
    }

    /** A tier-1 suit holding those upgrades, each in the first rectangle it fits. */
    static ItemStack suitWith(ItemLike... upgrades) {
        var suit = new ItemStack(GearContent.IRON_GEAR.get());
        var grid = Gear.grid(suit);
        for (var upgrade : upgrades) {
            var entry = GearUpgrade.of(upgrade.asItem().builtInRegistryHolder());
            grid = grid.placeFirstFit(entry.width(), entry.height(), new ItemStackTemplate(upgrade.asItem())).orElseThrow();
        }
        Gear.setGrid(suit, grid);
        return suit;
    }

    /** The suit's energy capability, as another mod's charger finds it. */
    static EnergyHandler energy(ItemStack suit) {
        return ItemAccess.forStack(suit).getCapability(Capabilities.Energy.ITEM);
    }

    /** Fills the suit through its capability, and gives back how much it took. */
    static int charge(ItemStack suit, int amount) {
        try (var transaction = Transaction.openRoot()) {
            int inserted = energy(suit).insert(amount, transaction);
            transaction.commit();
            return inserted;
        }
    }

    static GearUpgrade upgrade(ItemLike item) {
        return GearUpgrade.of(item.asItem().builtInRegistryHolder());
    }

    /** A player standing on the platform and wearing the suit, ticked by the test each game tick. */
    static Player wearer(GameTestHelper helper, ItemStack suit) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var feet = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
        player.setPos(feet.x, feet.y, feet.z);
        player.setItemSlot(EquipmentSlot.CHEST, suit);
        return player;
    }
}
