// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

/**
 * What a shield takes of one hit and the FE it spends for it, free of Minecraft so it runs on a plain
 * JVM. The shield takes its fraction of the hit and pays for each point taken.
 *
 * @param absorbed the damage the shield takes, which the wearer does not
 * @param spent    the FE the shield spends for it
 */
public record Shielding(float absorbed, int spent) {

    /** A hit the shield takes nothing of. */
    public static final Shielding NONE = new Shielding(0, 0);

    /**
     * What a shield taking {@code fraction} of a hit of {@code damage} takes, with {@code energy} FE
     * in the suit and {@code fePerPoint} FE spent for each point of damage it takes.
     */
    public static Shielding absorb(float damage, double fraction, int energy, int fePerPoint) {
        if (energy <= 0) {
            return NONE;
        }
        double absorbed = damage * fraction;
        if (fePerPoint > 0) {
            absorbed = Math.min(absorbed, (double) energy / fePerPoint);
        }
        // Rounded up so no point is taken for free, and never more than the suit holds.
        int spent = (int) Math.min(energy, Math.ceil(absorbed * fePerPoint));
        return new Shielding((float) absorbed, spent);
    }
}
