// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.bench;

import java.util.Optional;

import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.gear.GearGridMenu;
import io.github._5thlayer.gearworks.gear.LevelCost;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.jspecify.annotations.Nullable;

/**
 * A Gear Bench's menu: the suit's slot, a slot per stocked material, then the player's inventory. A
 * click on a cell of the suit's grid, sent as a menu button as the Gear Grid's is, sets the bench to
 * raise the upgrade covering it; the {@link #RAISE} button raises it one Level. The selected cell and
 * the stocked FE ride in data slots, whole ints under NeoForge, the FE as its two halves.
 */
public final class GearBenchMenu extends AbstractContainerMenu {

    /** The button that raises the selected upgrade, past every cell's button. */
    public static final int RAISE = GearGridMenu.CELL_STRIDE * GearGridMenu.CELL_STRIDE;

    public static final int SUIT_SLOT = 0;
    public static final int FIRST_STOCK_SLOT = 1;
    public static final int FIRST_PLAYER_SLOT = FIRST_STOCK_SLOT + LevelCost.MAX_MATERIALS;

    public static final int SUIT_LEFT = 8;
    public static final int GRID_LEFT = 30;
    public static final int GRID_TOP = 30;
    /** The grid is drawn within a square this wide, its cells shrunk to fit a grid wider than five. */
    public static final int GRID_AREA = 5 * GearGridMenu.CELL;
    public static final int STOCK_LEFT = 128;
    public static final int STOCK_TOP = 30;
    public static final int STOCK_STEP = 20;
    public static final int WIDTH = 216;
    public static final int INVENTORY_LEFT = (WIDTH - 9 * GearGridMenu.CELL + 2) / 2;
    public static final int INVENTORY_TOP = 150;

    private static final int DATA_SELECTED_X = 0;
    private static final int DATA_SELECTED_Y = 1;
    private static final int DATA_ENERGY_LOW = 2;
    private static final int DATA_ENERGY_HIGH = 3;
    private static final int DATA_COUNT = 4;

    /** The bench on the server; null on the client, which reads what the server syncs. */
    private final @Nullable GearBenchBlockEntity bench;
    private final ContainerLevelAccess access;
    private final ItemStacksResourceHandler suit;
    private final BenchStock stock;
    private final ContainerData data;

    GearBenchMenu(int containerId, Inventory inventory, GearBenchBlockEntity bench) {
        this(containerId, inventory, bench, ContainerLevelAccess.create(bench.getLevel(), bench.getBlockPos()), bench.suitSlot(),
                bench.stock(), serverData(bench));
    }

    /**
     * The client's menu, whose slots and data stand over copies the sync fills. It checks no distance
     * of its own, and needs none of the position the server writes: the server closes it.
     */
    public GearBenchMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, null, ContainerLevelAccess.NULL, new GearBenchBlockEntity.SuitSlot(previous -> {
        }), new BenchStock(() -> null, () -> {
        }), new SimpleContainerData(DATA_COUNT));
    }

    private GearBenchMenu(int containerId, Inventory inventory, @Nullable GearBenchBlockEntity bench, ContainerLevelAccess access,
            ItemStacksResourceHandler suit, BenchStock stock, ContainerData data) {
        super(GearContent.GEAR_BENCH_MENU.get(), containerId);
        this.bench = bench;
        this.access = access;
        this.suit = suit;
        this.stock = stock;
        this.data = data;
        addSlot(new ResourceHandlerSlot(suit, suit::set, 0, SUIT_LEFT, GRID_TOP));
        for (int i = 0; i < LevelCost.MAX_MATERIALS; i++) {
            addSlot(new ResourceHandlerSlot(stock, stock::set, i, STOCK_LEFT, STOCK_TOP + i * STOCK_STEP));
        }
        addStandardInventorySlots(inventory, INVENTORY_LEFT, INVENTORY_TOP);
        addDataSlots(data);
    }

    private static ContainerData serverData(GearBenchBlockEntity bench) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_SELECTED_X -> bench.selectedX();
                    case DATA_SELECTED_Y -> bench.selectedY();
                    case DATA_ENERGY_LOW -> (int) bench.energy();
                    case DATA_ENERGY_HIGH -> (int) (bench.energy() >>> 32);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    /** The suit the bench holds, or an empty stack. */
    public ItemStack suit() {
        return suit.getResource(0).toStack(suit.getAmountAsInt(0));
    }

    /** The upgrade the bench is set to raise, with its next Level's cost. */
    public Optional<BenchTarget> target() {
        return BenchTarget.of(suit(), data.get(DATA_SELECTED_X), data.get(DATA_SELECTED_Y));
    }

    /** The FE the bench has stocked. */
    public long energy() {
        return ((long) data.get(DATA_ENERGY_HIGH) << 32) | (data.get(DATA_ENERGY_LOW) & 0xFFFFFFFFL);
    }

    /** How much of the material at that index of the next Level's cost the bench has stocked. */
    public long stocked(int index) {
        return target().map(target -> target.stocked(stock, index)).orElse(0L);
    }

    /** Whether the whole of the next Level's cost is stocked, so {@link #RAISE} raises it. */
    public boolean ready() {
        return target().map(target -> target.stocked(stock, energy())).orElse(false);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (bench == null || id < 0) {
            return false;
        }
        if (id == RAISE) {
            return bench.raise();
        }
        return bench.select(id % GearGridMenu.CELL_STRIDE, id / GearGridMenu.CELL_STRIDE);
    }

    /** A shift-click moves a suit to its slot and a material to the stock, and either back to the inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        var stack = slot.getItem();
        var moved = stack.copy();
        if (index < FIRST_PLAYER_SLOT) {
            if (!moveItemStackTo(stack, FIRST_PLAYER_SLOT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, SUIT_SLOT, FIRST_PLAYER_SLOT, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, GearContent.GEAR_BENCH.get());
    }
}
