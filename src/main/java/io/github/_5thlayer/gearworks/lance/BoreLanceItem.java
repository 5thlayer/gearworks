// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.lance;

import java.util.function.Consumer;

import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
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
 * worn out; a tier that runs on FE has no durability, and dry lacks the FE for its Lunge or a Bore.
 * Either mines single blocks without, and neither lunges nor bores until repaired or charged.
 */
public final class BoreLanceItem extends Item {

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
        int max = stack.getOrDefault(DataComponents.MAX_DAMAGE, 0);
        return upkeep == Upkeep.WEARS_OUT && max > 0 && stack.getOrDefault(DataComponents.DAMAGE, 0) >= max - 1;
    }

    /** The level of Lunge the stack fires: its tier's, unless it is worn out or lacks the FE for the lunge. */
    private int lungeOf(ItemInstance stack) {
        if (upkeep == Upkeep.ENERGY) {
            return stack.getOrDefault(GearContent.ENERGY.get(), 0) >= lungeEnergy() ? lunge : 0;
        }
        return wornOut(stack) ? 0 : lunge;
    }

    /** The FE a lunge costs the tier: its price a level, at the tier's level. */
    int lungeEnergy() {
        return (int) Math.min(Integer.MAX_VALUE, (long) lunge * GearworksConfig.lanceLungeEnergy());
    }

    /**
     * Mines the block, paying for it, then breaks the rest of the Bore while the lance lasts; a block its
     * Lunge breaks costs Lunge Boring's price instead, and is one block alone.
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (LungeBoring.breaking()) {
            stack.hurtAndBreak(GearworksConfig.lungeBoringCost(), owner, EquipmentSlot.MAINHAND);
            BoreLance.spend(stack, GearworksConfig.lanceLungeBoringEnergy());
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
        if (upkeep != Upkeep.WEARS_OUT) {
            return amount;
        }
        return Math.max(0, Math.min(amount, stack.getMaxDamage() - 1 - stack.getDamageValue()));
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
        if (upkeep == Upkeep.ENERGY && (enchantment.is(Enchantments.UNBREAKING) || enchantment.is(Enchantments.MENDING))) {
            return false;
        }
        return !enchantment.is(Enchantments.LUNGE) && super.supportsEnchantment(stack, enchantment);
    }

    /** How a tier pays for what it does. */
    public enum Upkeep {
        /** With durability, and breaks when it runs out. */
        BREAKS,
        /** With durability, and stops at its last, worn out, until repaired. */
        WEARS_OUT,
        /** With FE and no durability. */
        ENERGY
    }
}
