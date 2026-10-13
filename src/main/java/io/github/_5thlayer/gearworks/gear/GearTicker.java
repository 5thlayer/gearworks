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
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The worn suit's Stats, each server tick: solar charges it in daylight under open sky; night vision
 * runs while it has the FE to draw each tick; speed raises walking speed while it has the FE to draw,
 * drawing it while the player walks; jump raises jump strength while it has the FE for a jump,
 * which each boosted jump spends; and flight lets the player fly while it has the FE to draw each tick
 * in the air, and lets them down slowly when it runs dry there.
 */
public final class GearTicker {

    /** The walking speed modifier the suit's speed upgrades give. */
    public static final Identifier SPEED = Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "gear_speed");

    /** The jump strength modifier the suit's jump upgrades give. */
    public static final Identifier JUMP = Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "gear_jump");

    /** The creative flight modifier the suit's flight upgrade gives. */
    public static final Identifier FLIGHT = Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "gear_flight");

    // A player standing still still reports a little movement from the client's rounding.
    private static final double WALKING = 1.0E-5;

    // Over 200 ticks, under which the screen starts to flicker as night vision runs out.
    private static final int NIGHT_VISION_TICKS = 400;
    private static final int NIGHT_VISION_REFRESH = 220;

    // Slow falling as the suit lets a player down, topped up until they land.
    private static final int SLOW_FALL_TICKS = 40;
    private static final int SLOW_FALL_REFRESH = 20;

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
            takeNightVision(player);
            takeFlight(player);
            letDown(player);
            return;
        }
        var totals = Gear.totals(suit);
        int energy = Gear.energy(suit);
        int before = energy;
        if (totals.amount(Stat.SOLAR) > 0 && inSunlight(player)) {
            // Never down: an open grid may hold FE over what its batteries now take.
            energy = Math.max(energy, (int) Math.min(Gear.capacity(suit), energy + totals.amount(Stat.SOLAR)));
        }
        if (totals.canDraw(Stat.NIGHT_VISION, energy)) {
            energy -= totals.draw(Stat.NIGHT_VISION);
            giveNightVision(player);
        } else {
            takeNightVision(player);
        }
        energy = fly(player, totals, energy);
        letDown(player);
        boolean fast = totals.canDraw(Stat.SPEED, energy);
        modify(player, Attributes.MOVEMENT_SPEED, SPEED, fast ? totals.underCeiling(Stat.SPEED) : 0);
        if (fast && player.getKnownMovement().horizontalDistanceSqr() > WALKING) {
            energy -= totals.draw(Stat.SPEED);
        }
        boolean springy = totals.canDraw(Stat.JUMP, energy);
        modify(player, Attributes.JUMP_STRENGTH, JUMP, springy ? totals.underCeiling(Stat.JUMP) : 0);
        if (energy != before) {
            Gear.setEnergy(suit, energy);
        }
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

    /**
     * Lets the player fly while the suit has the FE for it, drawing it each tick they fly unless their
     * game mode lets them fly anyway, and takes flight away once it has not; gives back the FE left.
     */
    private static int fly(Player player, Gear.Totals totals, int energy) {
        if (!totals.canDraw(Stat.FLIGHT, energy)) {
            takeFlight(player);
            return energy;
        }
        var flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null && !flight.hasModifier(FLIGHT)) {
            flight.addTransientModifier(new AttributeModifier(FLIGHT, 1, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getAbilities().flying && !player.isCreative() && !player.isSpectator()) {
            energy -= totals.draw(Stat.FLIGHT);
        }
        return energy;
    }

    /**
     * Takes away the flight the suit gave, and none a game mode or another mod gave; a player it leaves
     * in the air stops flying and comes down slowly, at no cost.
     */
    private static void takeFlight(Player player) {
        var flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight == null || !flight.hasModifier(FLIGHT)) {
            return;
        }
        flight.removeModifier(FLIGHT);
        if (player.mayFly() || player.onGround()) {
            return;
        }
        if (player.getAbilities().flying) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        slowFall(player);
    }

    /** Keeps up the slow falling the suit gave until the player lands or swims, then takes it away. */
    private static void letDown(Player player) {
        var effect = player.getEffect(MobEffects.SLOW_FALLING);
        if (effect == null || !isFromSuit(effect, SLOW_FALL_TICKS)) {
            return;
        }
        if (player.onGround() || player.isInWater()) {
            player.removeEffect(MobEffects.SLOW_FALLING);
        } else if (effect.getDuration() <= SLOW_FALL_REFRESH) {
            slowFall(player);
        }
    }

    private static void slowFall(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0, true, false, true));
    }

    /** Keeps night vision on, topping it up before it runs low, unless another source gave it. */
    private static void giveNightVision(Player player) {
        var effect = player.getEffect(MobEffects.NIGHT_VISION);
        if (effect == null || isFromSuit(effect, NIGHT_VISION_TICKS) && effect.getDuration() <= NIGHT_VISION_REFRESH) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_TICKS, 0, true, false, true));
        }
    }

    /** Takes away the suit's night vision, and none a potion or another mod gave. */
    private static void takeNightVision(Player player) {
        var effect = player.getEffect(MobEffects.NIGHT_VISION);
        if (effect != null && isFromSuit(effect, NIGHT_VISION_TICKS)) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    /**
     * The suit's effects are ambient and show no particles, which a potion's never are, and last no longer
     * than the suit gives them for.
     */
    private static boolean isFromSuit(MobEffectInstance effect, int ticks) {
        return effect.isAmbient() && !effect.isVisible() && effect.getAmplifier() == 0 && effect.getDuration() <= ticks;
    }

    private static boolean inSunlight(Player player) {
        var level = player.level();
        return level.isBrightOutside() && level.canSeeSky(BlockPos.containing(player.getEyePosition()));
    }
}
