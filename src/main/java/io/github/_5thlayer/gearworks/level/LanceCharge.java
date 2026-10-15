// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.level;

/**
 * What a worn Suit gives a netherite Bore Lance in the player's hand in one tick, free of Minecraft so it
 * runs on a plain JVM: up to its rate, only from what the Suit holds over its reserve, a fraction of what
 * its batteries hold, and no more than the lance has room for.
 */
public final class LanceCharge {

    private LanceCharge() {
    }

    /**
     * The FE a Suit holding {@code suitEnergy} of {@code suitCapacity} moves in a tick into a lance holding
     * {@code lanceEnergy} of {@code lanceCapacity}, at up to {@code rate}, keeping {@code reserve} of its
     * capacity: a Suit at or under its reserve moves nothing, and none goes under it.
     */
    public static int transfer(int suitEnergy, int suitCapacity, int lanceEnergy, int lanceCapacity, int rate, double reserve) {
        long kept = (long) Math.ceil(suitCapacity * reserve);
        long spare = suitEnergy - kept;
        long room = (long) lanceCapacity - lanceEnergy;
        return (int) Math.max(0, Math.min(rate, Math.min(spare, room)));
    }
}
