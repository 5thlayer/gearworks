// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.bench;

import java.util.function.LongSupplier;

import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The FE a Gear Bench has stocked toward the next Level. Any mod's cable fills it up to that Level's
 * FE and takes nothing back; it holds a long, since a high Level costs more FE than an int carries.
 * FE stocked for one upgrade stays when the bench is set to another, and counts toward that one.
 */
final class BenchEnergy implements EnergyHandler {

    private final LongSupplier need;
    private final Runnable changed;
    private final Journal journal = new Journal();
    private long amount;

    /**
     * @param need    the next Level's FE, or 0 while the bench has nothing to raise
     * @param changed called after the stock changes, to save the bench
     */
    BenchEnergy(LongSupplier need, Runnable changed) {
        this.need = need;
        this.changed = changed;
    }

    @Override
    public long getAmountAsLong() {
        return amount;
    }

    @Override
    public long getCapacityAsLong() {
        return need.getAsLong();
    }

    @Override
    public int insert(int offered, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(offered);
        long room = need.getAsLong() - amount;
        int inserted = (int) Math.min(offered, Math.max(0, room));
        if (inserted > 0) {
            journal.updateSnapshots(transaction);
            amount += inserted;
        }
        return inserted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return 0;
    }

    /** Sets the stock outright, as spending a Level's cost or loading the bench does. */
    void set(long amount) {
        if (this.amount != amount) {
            this.amount = amount;
            changed.run();
        }
    }

    private final class Journal extends SnapshotJournal<Long> {

        @Override
        protected Long createSnapshot() {
            return amount;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            amount = snapshot;
        }

        @Override
        protected void onRootCommit(Long originalState) {
            if (amount != originalState) {
                changed.run();
            }
        }
    }
}
