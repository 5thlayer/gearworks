// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

/**
 * A Railgun's Charge, tick by tick, and its shot's damage, free of Minecraft so they run on a plain JVM.
 * Held, the Charge advances a tick at a time, and on its last tick makes the Railgun Charged and pays its
 * cost, or waits at full until the Railgun holds it; put away, it keeps its progress; Charged, nothing
 * changes until it fires. Quick Charge shortens it as it does a crossbow's loading, and Power raises the
 * damage as it does a bow's arrows'.
 */
public final class RailgunCharge {

    /** The ticks Quick Charge takes off the Charge for each of its levels. */
    public static final int QUICK_CHARGE_TICKS = 5;

    /** The ticks Quick Charge never shortens the Charge under. */
    public static final int FLOOR = 10;

    private RailgunCharge() {
    }

    /**
     * The Charge a tick on: from {@code progress} ticks into it, whether the Railgun is {@code held} and
     * {@code charged}, holding {@code energy} FE, for a Charge costing {@code cost} FE whose base length is
     * {@code baseTicks}, at that level of Quick Charge.
     */
    public static Step tick(int progress, boolean held, boolean charged, int energy, int cost, int baseTicks, int quickCharge) {
        if (!held || charged) {
            return new Step(progress, charged, 0);
        }
        int ticks = ticks(baseTicks, quickCharge);
        int next = Math.min(ticks, progress + 1);
        if (next < ticks) {
            return new Step(next, false, 0);
        }
        return energy >= cost ? new Step(ticks, true, cost) : new Step(ticks, false, 0);
    }

    /**
     * The ticks a Charge of {@code baseTicks} takes at that level of Quick Charge: {@link #QUICK_CHARGE_TICKS}
     * fewer a level, never under {@link #FLOOR}, and never longer than its base.
     */
    public static int ticks(int baseTicks, int quickCharge) {
        return Math.max(Math.min(baseTicks, FLOOR), baseTicks - QUICK_CHARGE_TICKS * quickCharge);
    }

    /** The shot's damage at that level of Power: a quarter more a level, and a quarter more again, as a bow adds it. */
    public static double damage(double base, int power) {
        return power > 0 ? base * (1 + 0.25 * (power + 1)) : base;
    }

    /** The Charge after a tick: its progress, whether the Railgun is Charged, and the FE the tick spent. */
    public record Step(int progress, boolean charged, int spent) {
    }
}
