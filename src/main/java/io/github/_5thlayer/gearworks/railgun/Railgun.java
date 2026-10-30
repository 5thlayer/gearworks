// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.railgun;

import io.github._5thlayer.gearworks.Gearworks;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.level.RailgunCharge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The Railgun as a stack: the FE it holds, how far into its Charge it is, and whether it is Charged. Each
 * server tick a Railgun in either of a player's hands takes its Charge a tick on, as {@link RailgunCharge}
 * rules; one anywhere else keeps its state as it is.
 */
public final class Railgun {

    /** The enchantments a Railgun takes: by default Quick Charge, which shortens its Charge, and Power, which raises its damage. */
    public static final TagKey<Enchantment> ENCHANTABLE = TagKey.create(Registries.ENCHANTMENT, id("railgun"));

    /** The damage its shot does: a projectile's, which armor, shields and an enderman's dodge all meet as an arrow's. */
    public static final ResourceKey<DamageType> SHOT = ResourceKey.create(Registries.DAMAGE_TYPE, id("railgun"));

    private Railgun() {
    }

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof RailgunItem;
    }

    /** The FE the Railgun holds. */
    public static int energy(ItemStack railgun) {
        return railgun.getOrDefault(GearContent.ENERGY.get(), 0);
    }

    /** Whether the Railgun is Charged: its next shot loaded and paid for. */
    public static boolean charged(ItemStack railgun) {
        return railgun.has(GearContent.CHARGED.get());
    }

    /** The ticks of its Charge the Railgun has loaded. */
    static int progress(ItemStack railgun) {
        return railgun.getOrDefault(GearContent.RAILGUN_CHARGE.get(), 0);
    }

    /** Empties it once it fires: no longer Charged, and its Charge starts again from nothing. */
    static void unload(ItemStack railgun) {
        railgun.remove(GearContent.CHARGED.get());
        railgun.remove(GearContent.RAILGUN_CHARGE.get());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        for (var hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (is(stack)) {
                charge(player, stack);
            }
        }
    }

    /** Takes a held Railgun's Charge a tick on, paying for it on its last, with a crossbow's click once Charged. */
    private static void charge(Player player, ItemStack railgun) {
        boolean charged = charged(railgun);
        int progress = progress(railgun);
        var step = RailgunCharge.tick(progress, true, charged, energy(railgun), GearworksConfig.railgunChargeEnergy(),
                GearworksConfig.railgunChargeTicks(), level(player.level(), railgun, Enchantments.QUICK_CHARGE));
        if (step.progress() != progress) {
            railgun.set(GearContent.RAILGUN_CHARGE.get(), step.progress());
        }
        if (step.spent() > 0) {
            railgun.set(GearContent.ENERGY.get(), energy(railgun) - step.spent());
        }
        if (step.charged() && !charged) {
            railgun.set(GearContent.CHARGED.get(), Unit.INSTANCE);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END,
                    SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    /** The Railgun's level of that enchantment. */
    static int level(Level level, ItemStack railgun, ResourceKey<Enchantment> enchantment) {
        return railgun.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, path);
    }
}
