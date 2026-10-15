// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.lance;

import java.util.function.Consumer;

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
 * off and no book raises, and a mining tool whose widest Bore is the tier's. A tier that never breaks
 * stops at one durability left, as an elytra does, and is then worn out: it mines single blocks but
 * neither lunges nor bores until it is repaired.
 */
public final class BoreLanceItem extends Item {

    private final Bore widest;
    private final int lunge;
    private final boolean neverBreaks;

    public BoreLanceItem(Properties properties, Bore widest, int lunge, boolean neverBreaks) {
        super(properties);
        this.widest = widest;
        this.lunge = lunge;
        this.neverBreaks = neverBreaks;
    }

    /** The tier's own Bore, the widest its key steps through. */
    public Bore widest() {
        return widest;
    }

    /** The level of the tier's built-in Lunge. */
    public int lunge() {
        return lunge;
    }

    /** Whether the tier stops at one durability left instead of breaking. */
    public boolean neverBreaks() {
        return neverBreaks;
    }

    /** Whether the stack is a tier that never breaks, at the one durability it stops at. */
    boolean wornOut(ItemInstance stack) {
        int max = stack.getOrDefault(DataComponents.MAX_DAMAGE, 0);
        return neverBreaks && max > 0 && stack.getOrDefault(DataComponents.DAMAGE, 0) >= max - 1;
    }

    /**
     * Mines the block, paying for it, then breaks the rest of the Bore while the lance lasts; a block its
     * Lunge breaks costs Lunge Boring's price instead, and is one block alone.
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (LungeBoring.breaking()) {
            stack.hurtAndBreak(GearworksConfig.lungeBoringCost(), owner, EquipmentSlot.MAINHAND);
            return true;
        }
        boolean mined = super.mineBlock(stack, level, state, pos, owner);
        if (owner instanceof ServerPlayer player) {
            BoreBreaker.bore(stack, player, pos, state);
        }
        return mined;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        if (!neverBreaks) {
            return amount;
        }
        return Math.max(0, Math.min(amount, stack.getMaxDamage() - 1 - stack.getDamageValue()));
    }

    @Override
    public int getEnchantmentLevel(ItemInstance stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.LUNGE)) {
            return wornOut(stack) ? 0 : lunge;
        }
        return super.getEnchantmentLevel(stack, enchantment);
    }

    @Override
    public ItemEnchantments getAllEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
        var enchantments = new ItemEnchantments.Mutable(super.getAllEnchantments(stack, lookup));
        enchantments.set(lookup.getOrThrow(Enchantments.LUNGE), wornOut(stack) ? 0 : lunge);
        return enchantments.toImmutable();
    }

    /** Lunge is built in, so a book or a table can't add it. */
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return !enchantment.is(Enchantments.LUNGE) && super.supportsEnchantment(stack, enchantment);
    }
}
