// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.bench;

import java.util.Optional;
import java.util.function.Consumer;

import io.github._5thlayer.gearworks.gear.Gear;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.grid.Placement;
import io.github._5thlayer.gearworks.level.CostCurve.Cost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * A placed Gear Bench: the suit it holds, the upgrade in that suit's grid it is set to raise, and the
 * materials and FE stocked toward that upgrade's next Level. Automation stocks it through the item and
 * energy capabilities, on every face; the player raises one Level at a time, which spends exactly the
 * Level's cost and only once all of it is stocked.
 */
public final class GearBenchBlockEntity extends BlockEntity implements MenuProvider {

    private static final String SUIT_KEY = "suit";
    private static final String STOCK_KEY = "stock";
    private static final String ENERGY_KEY = "energy";
    private static final String SELECTED_X_KEY = "selected_x";
    private static final String SELECTED_Y_KEY = "selected_y";
    private static final String SHOWN_KEY = "shown";

    private final ItemStacksResourceHandler suit = new SuitSlot(this::suitChanged);
    private final BenchStock stock = new BenchStock(() -> target().map(BenchTarget::cost).orElse(null), this::setChanged);
    private final BenchEnergy energy = new BenchEnergy(() -> target().map(BenchTarget::cost).map(Cost::energy).orElse(0L), this::setChanged);
    /** What another mod's pipes and hoppers see: they fill the stock and take nothing back. */
    private final ResourceHandler<ItemResource> stockFace = new DelegatingResourceHandler<>(stock) {
        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
    };

    /** The top-left cell of the upgrade the bench is set to raise, or -1 with none. */
    private int selectedX = -1;
    private int selectedY = -1;
    /** While {@link #raise} puts the raised suit back, which is no new suit. */
    private boolean raising;
    /** On a watcher's copy, the suit the bench was last sent holding, which floats above it. */
    private ItemStack shown = ItemStack.EMPTY;

    public GearBenchBlockEntity(BlockPos pos, BlockState state) {
        super(GearContent.GEAR_BENCH_ENTITY.get(), pos, state);
    }

    public ResourceHandler<ItemResource> stockFace() {
        return stockFace;
    }

    public EnergyHandler energyFace() {
        return energy;
    }

    ItemStacksResourceHandler suitSlot() {
        return suit;
    }

    BenchStock stock() {
        return stock;
    }

    public long energy() {
        return energy.getAmountAsLong();
    }

    /** A copy of the suit the bench holds, or an empty stack. */
    public ItemStack suit() {
        return suit.getResource(0).toStack(suit.getAmountAsInt(0));
    }

    /** The suit a watcher's copy of the bench draws floating above it, or an empty stack. */
    public ItemStack shown() {
        return shown;
    }

    public int selectedX() {
        return selectedX;
    }

    public int selectedY() {
        return selectedY;
    }

    /** The upgrade the bench is set to raise, if it holds a suit and is set to one. */
    public Optional<BenchTarget> target() {
        return BenchTarget.of(suit(), selectedX, selectedY);
    }

    /** Sets the bench to raise the upgrade covering that cell; false if none does. */
    public boolean select(int x, int y) {
        var held = suit();
        var placement = Gear.is(held) ? Gear.grid(held).at(x, y) : Optional.<Placement<ItemStackTemplate>>empty();
        if (placement.isEmpty()) {
            return false;
        }
        selectedX = placement.get().x();
        selectedY = placement.get().y();
        setChanged();
        return true;
    }

    /**
     * Raises the upgrade the bench is set to by one Level, spending exactly that Level's cost from the
     * stock; false, spending nothing, unless all of it is stocked.
     */
    public boolean raise() {
        var target = target();
        if (target.isEmpty() || !target.get().stocked(stock, energy.getAmountAsLong())) {
            return false;
        }
        var cost = target.get().cost();
        for (int i = 0; i < cost.materials().size(); i++) {
            var resource = stock.getResource(i);
            stock.set(i, resource, stock.getAmountAsInt(i) - cost.materials().get(i).count());
        }
        energy.set(energy.getAmountAsLong() - cost.energy());
        var raised = suit();
        Gear.setGrid(raised, Gear.grid(raised).replace(target.get().placement(), target.get().raised()));
        raising = true;
        try {
            suit.set(0, ItemResource.of(raised), raised.getCount());
        } finally {
            raising = false;
        }
        return true;
    }

    /**
     * Any suit put in or taken out leaves the bench set to nothing; raising a Level keeps it set. Its
     * watchers are sent the suit either way, to draw.
     */
    private void suitChanged(ItemStack previous) {
        if (!raising) {
            selectedX = -1;
            selectedY = -1;
        }
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new GearBenchMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        suit.serialize(output.child(SUIT_KEY));
        stock.serialize(output.child(STOCK_KEY));
        output.putLong(ENERGY_KEY, energy.getAmountAsLong());
        output.putInt(SELECTED_X_KEY, selectedX);
        output.putInt(SELECTED_Y_KEY, selectedY);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        suit.deserialize(input.childOrEmpty(SUIT_KEY));
        stock.deserialize(input.childOrEmpty(STOCK_KEY));
        energy.set(Math.max(0, input.getLongOr(ENERGY_KEY, 0)));
        selectedX = input.getIntOr(SELECTED_X_KEY, -1);
        selectedY = input.getIntOr(SELECTED_Y_KEY, -1);
    }

    /** Only what its watchers draw: the suit it holds. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        var held = suit();
        if (!held.isEmpty()) {
            output.store(SHOWN_KEY, ItemStack.CODEC, held);
        }
        return output.buildResult();
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** A watcher reads only what {@link #getUpdateTag} sends, as the chunk arrives. */
    @Override
    public void handleUpdateTag(ValueInput input) {
        shown = input.read(SHOWN_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    /** A watcher reads only what {@link #getUpdateTag} sends, as it changes. */
    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        handleUpdateTag(input);
    }

    /** A broken bench drops its suit and its stocked materials; its FE is lost. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) {
            return;
        }
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), suit());
        for (int i = 0; i < stock.size(); i++) {
            var resource = stock.getResource(i);
            int left = stock.getAmountAsInt(i);
            while (left > 0 && !resource.isEmpty()) {
                int count = Math.min(left, resource.getMaxStackSize());
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), resource.toStack(count));
                left -= count;
            }
        }
    }

    /** The bench's one suit; the client's copy is told of no change. */
    static final class SuitSlot extends ItemStacksResourceHandler {

        private final Consumer<ItemStack> changed;

        SuitSlot(Consumer<ItemStack> changed) {
            super(1);
            this.changed = changed;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return Gear.is(resource.toStack());
        }

        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            changed.accept(previousContents);
        }
    }
}
