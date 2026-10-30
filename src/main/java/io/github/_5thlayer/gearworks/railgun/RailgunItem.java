// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.railgun;

import java.util.List;

import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.gear.TickingGear;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

/**
 * The Railgun: use fires it while it is Charged, and does nothing while it isn't; attack is a punch, as
 * with any item. Its bar shows the FE it holds, and it takes only the enchantments of
 * {@link Railgun#ENCHANTABLE}.
 */
public final class RailgunItem extends Item implements TickingGear {

    /** The bar's colour, an electric blue. */
    private static final int BAR_COLOR = 0x4FB8FF;

    public RailgunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var railgun = player.getItemInHand(hand);
        if (!Railgun.charged(railgun)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            RailgunShot.fire(server, player, railgun);
            Railgun.unload(railgun);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(MAX_BAR_WIDTH * (float) Railgun.energy(stack) / GearworksConfig.railgunCapacity()), 0, MAX_BAR_WIDTH);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(Railgun.ENCHANTABLE);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(Railgun.ENCHANTABLE);
    }

    /**
     * Its FE and its Charge change every tick it is held, and NeoForge restarts mining with an item that
     * has no durability whenever its components change, so neither alone resets the punch it mines with.
     */
    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !TickingGear.sameButForTicking(oldStack, newStack);
    }

    /** Nor does it lower and raise the Railgun again, every tick it charges, or when it fires. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !TickingGear.sameButForTicking(oldStack, newStack);
    }

    /** Its Charge, its FE as a Charge is paid for or a worn Suit tops it up, and whether it is Charged. */
    @Override
    public List<DataComponentType<?>> tickingComponents() {
        return List.of(GearContent.ENERGY.get(), GearContent.RAILGUN_CHARGE.get(), GearContent.CHARGED.get());
    }
}
