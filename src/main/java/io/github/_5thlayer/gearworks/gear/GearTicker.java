// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import io.github._5thlayer.gearworks.Gearworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The worn suit's Stats, each server tick: solar charges it in daylight under open sky; night vision
 * runs while it has the FE to draw each tick; speed raises walking speed while it has the FE to draw,
 * drawing it while the player walks; and jump raises jump strength while it has the FE for a jump,
 * which each boosted jump spends.
 */
public final class GearTicker {

    /** The walking speed modifier the suit's speed upgrades give. */
    public static final Identifier SPEED = Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "gear_speed");

    /** The jump strength modifier the suit's jump upgrades give. */
    public static final Identifier JUMP = Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "gear_jump");

    // A player standing still still reports a little movement from the client's rounding.
    private static final double WALKING = 1.0E-5;

    // Over 200 ticks, under which the screen starts to flicker as night vision runs out.
    private static final int NIGHT_VISION_TICKS = 400;
    private static final int NIGHT_VISION_REFRESH = 220;

    private GearTicker() {
    }

    static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (!player.level().isClientSide()) {
            tick(player);
        }
    }

    /** A boosted jump spends the suit's jump draw. */
    static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        var jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        var suit = player.getItemBySlot(EquipmentSlot.CHEST);
        if (jump == null || !jump.hasModifier(JUMP) || !Gear.is(suit)) {
            return;
        }
        int energy = Gear.energy(suit);
        Gear.setEnergy(suit, Math.max(0, energy - Gear.totals(suit).draw(Stat.JUMP)));
    }

    private static void tick(Player player) {
        var suit = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!Gear.is(suit)) {
            modify(player, Attributes.MOVEMENT_SPEED, SPEED, 0);
            modify(player, Attributes.JUMP_STRENGTH, JUMP, 0);
            stopSeeing(player);
            return;
        }
        var totals = Gear.totals(suit);
        int energy = Gear.energy(suit);
        int before = energy;
        if (totals.amount(Stat.SOLAR) > 0 && inSunlight(player)) {
            // Never down: an open grid may hold FE over what its batteries now take.
            energy = Math.max(energy, (int) Math.min(Gear.capacity(suit), energy + totals.amount(Stat.SOLAR)));
        }
        if (canDraw(totals, Stat.NIGHT_VISION, energy)) {
            energy -= totals.draw(Stat.NIGHT_VISION);
            see(player);
        } else {
            stopSeeing(player);
        }
        boolean fast = totals.amount(Stat.SPEED) > 0 && canDraw(totals, Stat.SPEED, energy);
        modify(player, Attributes.MOVEMENT_SPEED, SPEED, fast ? totals.underCeiling(Stat.SPEED, GearworksConfig.speedCeiling()) : 0);
        if (fast && player.getKnownMovement().horizontalDistanceSqr() > WALKING) {
            energy -= totals.draw(Stat.SPEED);
        }
        boolean springy = totals.amount(Stat.JUMP) > 0 && canDraw(totals, Stat.JUMP, energy);
        modify(player, Attributes.JUMP_STRENGTH, JUMP, springy ? totals.underCeiling(Stat.JUMP, GearworksConfig.jumpCeiling()) : 0);
        if (energy != before) {
            Gear.setEnergy(suit, energy);
        }
    }

    /** Whether the suit holds an upgrade of the Stat and the FE for its draw: an empty suit runs nothing. */
    private static boolean canDraw(Gear.Totals totals, Stat stat, int energy) {
        return totals.has(stat) && energy > 0 && energy >= totals.draw(stat);
    }

    /** Sets the modifier to that fraction of the attribute's base, or takes it away at 0. */
    private static void modify(Player player, Holder<Attribute> attribute, Identifier id, double fraction) {
        var instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (fraction > 0) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, fraction, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else {
            instance.removeModifier(id);
        }
    }

    /** Keeps night vision on, topping it up before it runs low, unless another source gave it. */
    private static void see(Player player) {
        var effect = player.getEffect(MobEffects.NIGHT_VISION);
        if (effect == null || isSuits(effect) && effect.getDuration() <= NIGHT_VISION_REFRESH) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_TICKS, 0, true, false, true));
        }
    }

    /** Takes away the suit's night vision, and none a potion or another mod gave. */
    private static void stopSeeing(Player player) {
        var effect = player.getEffect(MobEffects.NIGHT_VISION);
        if (effect != null && isSuits(effect)) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    /** The suit's night vision is ambient and shows no particles, which a potion's never is. */
    private static boolean isSuits(MobEffectInstance effect) {
        return effect.isAmbient() && !effect.isVisible() && effect.getAmplifier() == 0 && effect.getDuration() <= NIGHT_VISION_TICKS;
    }

    private static boolean inSunlight(Player player) {
        var level = player.level();
        return level.isBrightOutside() && level.canSeeSky(BlockPos.containing(player.getEyePosition()));
    }
}
