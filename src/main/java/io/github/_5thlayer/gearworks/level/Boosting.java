// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

/**
 * A player's Boost over time, free of Minecraft so it runs on a plain JVM: a Boost pushes for its
 * Thrust, and its Cooldown, which starts with it and is never shorter than the Thrust, holds the next
 * one back. It lives on the player, so a second suit fires no sooner.
 */
public final class Boosting {

    // FireworkRocketEntity's pull on a gliding player, each tick of its flight.
    private static final double PUSH = 0.1;
    private static final double PULL = 0.5;

    private double strength;
    private int thrustLeft;
    private int cooldownLeft;
    private int cooldown;

    /** Whether no Cooldown holds the next Boost back. */
    public boolean ready() {
        return cooldownLeft == 0;
    }

    /** Whether a Boost's Thrust is still pushing. */
    public boolean thrusting() {
        return thrustLeft > 0;
    }

    /** The speed, in blocks a tick, the Thrust pulls the player toward along their gaze. */
    public double strength() {
        return strength;
    }

    /** The ticks until the next Boost can fire. */
    public int cooldownLeft() {
        return cooldownLeft;
    }

    /** How much of the Cooldown is left, from 1 as a Boost fires to 0 once the next can. */
    public double cooldownFraction() {
        return cooldown == 0 ? 0 : (double) cooldownLeft / cooldown;
    }

    /** Starts a Boost: its Thrust for that many ticks, and a Cooldown of at least as many. */
    public void fire(double strength, int thrustTicks, int cooldown) {
        this.strength = strength;
        thrustLeft = thrustTicks;
        this.cooldown = Math.max(cooldown, thrustTicks);
        cooldownLeft = this.cooldown;
    }

    /** Ends the Thrust, as landing does; the Cooldown runs on, since the Boost was spent. */
    public void stopThrust() {
        thrustLeft = 0;
    }

    /** One tick of the Thrust and the Cooldown. */
    public void tick() {
        if (thrustLeft > 0) {
            thrustLeft--;
        }
        if (cooldownLeft > 0) {
            cooldownLeft--;
        }
    }

    /**
     * One axis of a glider's velocity after a tick of Thrust: vanilla's rocket step, which pulls it
     * toward {@code strength} along the gaze and settles a little over it.
     */
    public static double step(double velocity, double look, double strength) {
        return velocity + look * PUSH + (look * strength - velocity) * PULL;
    }

    /**
     * One axis of a velocity after a tick of Thrust, where the movement keeps only {@code drag} of it
     * each tick, as flight does: it undoes the drag first, so the Thrust settles where a glider's does
     * on every axis, however unevenly they drag.
     */
    public static double step(double velocity, double look, double strength, double drag) {
        return step(velocity / drag, look, strength);
    }
}
