// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

/**
 * What the Generators make together in one tick, free of Minecraft so it runs on a plain JVM: kinetic's
 * FE for the blocks moved on foot, combustion's while its fire burns, metabolic's while saturation
 * lasts, and how much of it all fits the suit. Solar's is {@link Solar}'s.
 */
public final class Generators {

    private static final double SATURATION_ROUNDING = 1.0E-3;

    private Generators() {
    }

    /**
     * A suit holding {@code energy} of {@code capacity} once {@code fe} is added: the whole FE that fits,
     * and the fraction of one over it, which the next tick adds to its own, so a Generator making 2.5 FE
     * a tick makes 5 every two. A full suit keeps no fraction, and one holding more than its capacity, as
     * an open grid may, loses none of what it holds.
     */
    public static Charge charge(int energy, int capacity, double fe) {
        int room = capacity - energy;
        if (room <= 0) {
            return new Charge(energy, 0);
        }
        if (fe >= room) {
            return new Charge(capacity, 0);
        }
        int whole = (int) fe;
        return new Charge(energy + whole, fe - whole);
    }

    /** The FE a suit holds after a tick of its Generators, and the fraction of one it keeps for the next. */
    public record Charge(int energy, double spare) {
    }

    /** The FE kinetic makes, at {@code amount} a block, for {@code blocks} moved on foot. */
    public static double kinetic(double amount, double blocks) {
        return amount * blocks;
    }

    /**
     * The whole FE metabolic makes in a tick: its {@code amount}, or less when the player's
     * {@code saturation}, at {@code energyPerPoint} FE a point, gives less, or the suit has less
     * {@code room}.
     */
    public static int metabolic(double amount, float saturation, int energyPerPoint, int room) {
        // Saturation is a float, so 0.01 of it reads 0.0099999...: a hair over makes it the whole FE it is worth.
        double fromSaturation = (double) saturation * energyPerPoint + SATURATION_ROUNDING;
        return (int) Math.max(0, Math.min(Math.min(amount, fromSaturation), room));
    }

    /** The saturation left once metabolic has made {@code fe} from it, at {@code energyPerPoint} FE a point. */
    public static float saturationAfter(float saturation, int fe, int energyPerPoint) {
        return Math.max(0, saturation - (float) fe / energyPerPoint);
    }

    /**
     * One tick of the suit's fire, holding {@code fire} ticks of burn time, under {@code upgrades}
     * combustion upgrades giving {@code amount} FE a tick together, in a suit with {@code room} FE to
     * fill: each upgrade burns a tick of it, a fire left with fewer ticks than upgrades gives that share
     * of the amount, and a suit nearly full burns only the ticks it has room for.
     */
    public static Burn burn(int fire, int upgrades, double amount, double room) {
        if (fire <= 0 || upgrades <= 0 || room <= 0) {
            return new Burn(Math.max(0, fire), 0);
        }
        double perTick = amount / upgrades;
        int burnt = Math.min(fire, upgrades);
        if (perTick > 0) {
            burnt = (int) Math.min(burnt, Math.ceil(room / perTick));
        }
        return new Burn(fire - burnt, perTick * burnt);
    }

    /** The burn time a fire has left after a tick, and the FE it gave. */
    public record Burn(int fire, double fe) {
    }
}
