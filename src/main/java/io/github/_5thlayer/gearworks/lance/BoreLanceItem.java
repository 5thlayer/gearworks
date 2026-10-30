// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.lance;

import java.util.List;
import java.util.function.Consumer;

import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.gear.TickingGear;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A tier of the Bore Lance: a spear with Lunge built in at the tier's level, which no grindstone takes
 * off and no book raises, and a mining tool whose widest Bore is the tier's. How it pays is its
 * {@link Upkeep}: a tier that never breaks stops at one durability left, as an elytra does, and is then
 * worn out; a tier that runs on FE has no durability, and is dry once it lacks the FE for a Bore's
 * block, lunging only while it holds a lunge's. Worn out or dry, it mines single blocks, and neither
 * lunges nor bores until repaired or charged.
 */
public final class BoreLanceItem extends Item implements TickingGear {

    private final Bore widest;
    private final int lunge;
    private final Upkeep upkeep;

    public BoreLanceItem(Properties properties, Bore widest, int lunge, Upkeep upkeep) {
        super(properties);
        this.widest = widest;
        this.lunge = lunge;
        this.upkeep = upkeep;
    }

    /** The tier's own Bore, the widest its key steps through. */
    public Bore widest() {
        return widest;
    }

    /** The level of the tier's built-in Lunge. */
    public int lunge() {
        return lunge;
    }

    /** How the tier pays for its Lunge and its Bores. */
    public Upkeep upkeep() {
        return upkeep;
    }

    /** Whether the stack is a tier that never breaks, at the one durability it stops at. */
    boolean wornOut(ItemInstance stack) {
        return upkeep.wornOut(stack);
    }

    /** Whether the stack can pay that FE for what it does next: on durability, whenever it isn't worn out. */
    boolean holds(ItemInstance stack, int fe) {
        return upkeep.holds(stack, fe);
    }

    /** The level of Lunge the stack fires: its tier's, unless it is worn out or lacks the FE for the lunge. */
    int lungeOf(ItemInstance stack) {
        return holds(stack, lungeEnergy()) ? lunge : 0;
    }

    /** The FE a lunge costs the tier: its price a level, at the tier's level. */
    int lungeEnergy() {
        return (int) Math.min(Integer.MAX_VALUE, (long) lunge * GearworksConfig.netheriteLanceLungeEnergy());
    }

    /**
     * Mines the block, paying for it, then breaks the rest of the Bore while the lance lasts; a block its
     * Lunge breaks costs Lunge Boring's price instead, and is one block alone.
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (LungeBoring.breaking()) {
            stack.hurtAndBreak(GearworksConfig.lungeBoringCost(), owner, EquipmentSlot.MAINHAND);
            BoreLance.spend(stack, GearworksConfig.netheriteLanceLungeBoringEnergy());
            return true;
        }
        boolean mined = super.mineBlock(stack, level, state, pos, owner);
        if (owner instanceof ServerPlayer player) {
            BoreBreaker.mined(stack, player, pos, state);
        }
        return mined;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return upkeep.damage(stack, amount);
    }

    @Override
    public int getEnchantmentLevel(ItemInstance stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.LUNGE)) {
            return lungeOf(stack);
        }
        return super.getEnchantmentLevel(stack, enchantment);
    }

    @Override
    public ItemEnchantments getAllEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
        var enchantments = new ItemEnchantments.Mutable(super.getAllEnchantments(stack, lookup));
        enchantments.set(lookup.getOrThrow(Enchantments.LUNGE), lungeOf(stack));
        return enchantments.toImmutable();
    }

    /**
     * Lunge is built in, so a book or a table can't add it; a tier on FE has no durability for Unbreaking
     * or Mending to act on.
     */
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return !enchantment.is(Enchantments.LUNGE) && upkeep.takes(enchantment) && super.supportsEnchantment(stack, enchantment);
    }

    /**
     * Charging changes a lance's FE every tick, and NeoForge restarts mining with an item that has no
     * durability whenever its components change, so the FE alone never resets mining.
     */
    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !TickingGear.sameButForTicking(oldStack, newStack);
    }

    /** Nor does it lower and raise the lance again, every tick it charges. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !TickingGear.sameButForTicking(oldStack, newStack);
    }

    /** A tier on FE charges from a worn Suit every tick it is held. */
    @Override
    public List<DataComponentType<?>> tickingComponents() {
        return List.of(GearContent.ENERGY.get());
    }

    /** How a tier pays for what it does, and what that decides: when it is worn out, what it holds, what durability it takes. */
    public enum Upkeep {
        /** With durability, and breaks when it runs out. */
        BREAKS,
        /** With durability, and stops at its last, worn out, until repaired. */
        WEARS_OUT {
            @Override
            boolean wornOut(ItemInstance stack) {
                int max = stack.getOrDefault(DataComponents.MAX_DAMAGE, 0);
                return max > 0 && stack.getOrDefault(DataComponents.DAMAGE, 0) >= max - 1;
            }

            @Override
            int damage(ItemStack stack, int amount) {
                return Math.max(0, Math.min(amount, stack.getMaxDamage() - 1 - stack.getDamageValue()));
            }
        },
        /** With FE and no durability. */
        ENERGY {
            @Override
            boolean holds(ItemInstance stack, int fe) {
                return stack.getOrDefault(GearContent.ENERGY.get(), 0) >= fe;
            }

            @Override
            boolean takes(Holder<Enchantment> enchantment) {
                return !enchantment.is(Enchantments.UNBREAKING) && !enchantment.is(Enchantments.MENDING);
            }
        };

        /** Whether the stack is stopped at its last durability: only a tier that wears out ever is. */
        boolean wornOut(ItemInstance stack) {
            return false;
        }

        /** Whether the stack can pay that FE for what it does next: on durability, whenever it isn't worn out. */
        boolean holds(ItemInstance stack, int fe) {
            return !wornOut(stack);
        }

        /** The durability the stack takes of that amount: all of it, unless that would wear it past its last. */
        int damage(ItemStack stack, int amount) {
            return amount;
        }

        /** Whether the enchantment has anything to act on: a tier on FE has no durability for Unbreaking or Mending. */
        boolean takes(Holder<Enchantment> enchantment) {
            return true;
        }
    }
}
