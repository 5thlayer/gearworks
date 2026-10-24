// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import io.github._5thlayer.gearworks.Gearworks;
import io.github._5thlayer.gearworks.level.Generators;
import io.github._5thlayer.gearworks.level.Solar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * A worn suit's Generators, each tick: solar in daylight under open sky, nuclear always, kinetic as the
 * player moves on foot, combustion while the suit's fire burns, and metabolic from the player's
 * saturation. Their FE adds up, and the fraction of one left over waits on the suit for the next tick.
 * None makes FE, nor spends fuel or saturation, once the suit is full.
 */
public final class SuitGenerators {

    /** The items combustion burns: a furnace burns more, and those stay the player's. */
    public static final TagKey<Item> SUIT_FUEL = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "suit_fuel"));

    // A player standing still still reports a little movement from the client's rounding.
    private static final double MOVING = 1.0E-5;

    private SuitGenerators() {
    }

    /** The suit's FE after its Generators have run this tick. */
    static int generate(Player player, ItemStack suit, Suit.Totals totals, int energy) {
        int capacity = Suit.capacity(suit);
        if (energy >= capacity) {
            suit.remove(GearContent.SPARE_FE.get());
            return energy;
        }
        double fe = suit.getOrDefault(GearContent.SPARE_FE.get(), 0.0) + solar(player, totals) + totals.amount(Stat.NUCLEAR)
                + kinetic(player, totals);
        fe += burn(player, suit, totals, capacity - energy - fe);
        fe += metabolize(player, totals, capacity - energy - fe);
        var charge = Generators.charge(energy, capacity, fe);
        if (charge.spare() > 0) {
            suit.set(GearContent.SPARE_FE.get(), charge.spare());
        } else {
            suit.remove(GearContent.SPARE_FE.get());
        }
        return charge.energy();
    }

    private static double solar(Player player, Suit.Totals totals) {
        double amount = totals.amount(Stat.SOLAR);
        if (amount <= 0) {
            return 0;
        }
        var level = player.level();
        return Solar.output(amount, level.isBrightOutside(), level.canSeeSky(BlockPos.containing(player.getEyePosition())));
    }

    private static double kinetic(Player player, Suit.Totals totals) {
        if (!totals.has(Stat.KINETIC) || !onFoot(player)) {
            return 0;
        }
        double moved = player.getKnownMovement().horizontalDistanceSqr();
        return moved > MOVING ? Generators.kinetic(totals.amount(Stat.KINETIC), Math.sqrt(moved)) : 0;
    }

    /**
     * Walking or sprinting, a jump's time in the air and wading through shallow water included: not
     * flying, gliding, swimming or riding.
     */
    private static boolean onFoot(Player player) {
        return !player.getAbilities().flying && !player.isFallFlying() && !player.isSwimming() && !player.isUnderWater()
                && !player.isPassenger();
    }

    /**
     * Burns a tick of the suit's fire, no more than the suit has {@code room} for, lighting the next fuel
     * item first when it is out; gives the FE it made.
     */
    private static double burn(Player player, ItemStack suit, Suit.Totals totals, double room) {
        if (!totals.has(Stat.COMBUSTION) || room <= 0) {
            return 0;
        }
        int fire = suit.getOrDefault(GearContent.COMBUSTION_FIRE.get(), 0);
        if (fire == 0) {
            fire = light(player);
        }
        if (fire == 0) {
            return 0;
        }
        var burn = Generators.burn(fire, totals.count(Stat.COMBUSTION), totals.amount(Stat.COMBUSTION), room);
        suit.set(GearContent.COMBUSTION_FIRE.get(), burn.fire());
        return burn.fe();
    }

    /**
     * Takes one fuel item from the player's main inventory, before the hotbar, and gives its furnace burn
     * time; 0 when there is none. Its remainder, as a bucket's, goes back to the player.
     */
    private static int light(Player player) {
        var inventory = player.getInventory();
        int hotbar = Inventory.getSelectionSize();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            int slot = (i + hotbar) % Inventory.INVENTORY_SIZE;
            var stack = inventory.getItem(slot);
            if (!stack.is(SUIT_FUEL)) {
                continue;
            }
            int burnTime = stack.getBurnTime(RecipeType.SMELTING, player.level().fuelValues());
            if (burnTime <= 0) {
                continue;
            }
            var remainder = stack.getCraftingRemainder();
            stack.shrink(1);
            if (remainder != null) {
                inventory.placeItemBackInInventory(remainder.create());
            }
            inventory.setChanged();
            return burnTime;
        }
        return 0;
    }

    /** Turns the player's saturation into no more FE than the suit has {@code room} for, and never their hunger. */
    private static double metabolize(Player player, Suit.Totals totals, double room) {
        if (!totals.has(Stat.METABOLIC) || room < 1) {
            return 0;
        }
        var food = player.getFoodData();
        int perPoint = GearworksConfig.metabolicEnergy();
        int made = Generators.metabolic(totals.amount(Stat.METABOLIC), food.getSaturationLevel(), perPoint, (int) room);
        if (made > 0) {
            food.setSaturation(Generators.saturationAfter(food.getSaturationLevel(), made, perPoint));
        }
        return made;
    }
}
