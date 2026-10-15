// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import io.github._5thlayer.gearworks.level.Boosting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The worn suit's Boost: a key's press throws the flying player along their gaze, as a firework does a
 * glider, for a Thrust of {@code boostTicks}, spending the suit's Boost draw and starting its Cooldown.
 * The server decides each Boost and tells the player's client, which moves the player; both run the
 * Thrust and the Cooldown each tick, so the HUD shows what the server holds. A player in creative or
 * spectator mode spends no FE and still waits out the Cooldown. Landing, or flight ending, stops the
 * Thrust.
 */
public final class SuitBoost {

    // A firework's spark, trailing the player's feet each tick of the Thrust.
    private static final double TRAIL = 0.05;

    // What flight keeps of the player's speed each tick: Player.travel keeps 0.6 of the vertical, and
    // LivingEntity.travelInAir 0.91 of the horizontal.
    private static final double HORIZONTAL_DRAG = 0.91;
    private static final double VERTICAL_DRAG = 0.6;

    private SuitBoost() {
    }

    /** The player's Boost and Cooldown, which no suit carries and no save keeps. */
    public static Boosting of(Player player) {
        return player.getData(GearContent.BOOSTING);
    }

    /** Whether a Boost would fire now: a flying player, a worn suit with a boost upgrade and its FE, and no Cooldown. */
    public static boolean canFire(Player player) {
        var suit = player.getItemBySlot(EquipmentSlot.CHEST);
        return Suit.is(suit) && canFire(player, suit, Suit.totals(suit));
    }

    private static boolean canFire(Player player, ItemStack suit, Suit.Totals totals) {
        if (!player.getAbilities().flying || !of(player).ready()) {
            return false;
        }
        return free(player) ? totals.has(Stat.BOOST) : totals.canDraw(Stat.BOOST, Suit.energy(suit));
    }

    /** Fires a Boost when one would fire, spending its FE and telling the player's client; says whether it fired. */
    public static boolean fire(Player player) {
        var suit = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!Suit.is(suit)) {
            return false;
        }
        var totals = Suit.totals(suit);
        if (!canFire(player, suit, totals)) {
            return false;
        }
        if (!free(player)) {
            Suit.setEnergy(suit, Suit.energy(suit) - totals.draw(Stat.BOOST));
        }
        double strength = totals.underCeiling(Stat.BOOST);
        int ticks = GearworksConfig.boostTicks();
        int cooldown = totals.cooldown(Stat.BOOST);
        of(player).fire(strength, ticks, cooldown);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 3.0f, 1.0f);
        if (player instanceof ServerPlayer server) {
            PacketDistributor.sendToPlayer(server, new BoostedPayload(strength, ticks, cooldown));
        }
        return true;
    }

    /** Before the player moves: a tick of Thrust while they fly, then a tick off the Thrust and the Cooldown. */
    static void onPlayerTick(PlayerTickEvent.Pre event) {
        var player = event.getEntity();
        var boosting = of(player);
        if (boosting.thrusting()) {
            if (player.getAbilities().flying) {
                thrust(player, boosting.strength());
            } else {
                boosting.stopThrust();
            }
        }
        boosting.tick();
    }

    private static void thrust(Player player, double strength) {
        var look = player.getLookAngle();
        var movement = player.getDeltaMovement();
        player.setDeltaMovement(new Vec3(
                Boosting.step(movement.x, look.x, strength, HORIZONTAL_DRAG),
                Boosting.step(movement.y, look.y, strength, VERTICAL_DRAG),
                Boosting.step(movement.z, look.z, strength, HORIZONTAL_DRAG)));
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY(), player.getZ(), 1, TRAIL, TRAIL, TRAIL, 0);
        }
    }

    /** A game mode that flies for free boosts for free. */
    private static boolean free(Player player) {
        return player.isCreative() || player.isSpectator();
    }
}
