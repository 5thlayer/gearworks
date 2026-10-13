// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * The Gear Grid of the worn suit, opened anywhere by a key. A click
 * on a cell, sent as a menu button, places the carried upgrade with its top-left there, or picks up
 * the upgrade covering it, so moving one is picking it up and placing it again. The server decides
 * every placement from the grid, never from the client.
 *
 * <p>The chest slot is in the menu, locked, so the suit's layout syncs to the client as it changes and
 * the suit cannot come off while its grid is open.
 */
public final class GearGridMenu extends AbstractContainerMenu {

    /** A cell's button id is {@code y * CELL_STRIDE + x}, past any grid's size. */
    public static final int CELL_STRIDE = 64;
    public static final int CELL = 18;
    /** The panel's width, a vanilla container's, which holds nine columns of slots. */
    public static final int WIDTH = 176;
    /** The grid's columns stand over the inventory's, nine of them at most ({@link GridSize#MAX_WIDTH}). */
    public static final int GRID_LEFT = 8;
    /** Below the suit's slot, the title and the energy line, which share the top row. */
    public static final int GRID_TOP = 30;
    public static final int SUIT_LEFT = 8;
    public static final int SUIT_TOP = 8;
    /** Where the title and the energy line start, right of the suit's slot. */
    public static final int LABEL_LEFT = 30;
    public static final int INVENTORY_LEFT = GRID_LEFT;

    /** The chest slot's index in the player's inventory, where a worn suit is. */
    public static final int CHEST = Inventory.EQUIPMENT_SLOT_MAPPING.int2ObjectEntrySet().stream()
            .filter(entry -> entry.getValue() == EquipmentSlot.CHEST)
            .mapToInt(Int2ObjectMap.Entry::getIntKey)
            .findFirst()
            .orElseThrow();

    private final Inventory inventory;
    private final int gridWidth;
    private final int gridHeight;
    private final int inventoryTop;

    GearGridMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory);
    }

    public GearGridMenu(int containerId, Inventory inventory) {
        super(GearContent.GRID_MENU.get(), containerId);
        this.inventory = inventory;
        var size = GridSize.of(suit().typeHolder());
        gridWidth = size.width();
        gridHeight = size.height();
        inventoryTop = GRID_TOP + Math.max(gridHeight, 1) * CELL + 14;
        addSlot(new Locked(inventory, CHEST, SUIT_LEFT, SUIT_TOP));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addInventorySlot(column + row * 9 + 9, INVENTORY_LEFT + column * CELL, inventoryTop + row * CELL);
            }
        }
        for (int column = 0; column < 9; column++) {
            addInventorySlot(column, INVENTORY_LEFT + column * CELL, inventoryTop + 3 * CELL + 4);
        }
    }

    /** Opens the Gear Grid of the suit the player wears, if they wear one. */
    public static void open(ServerPlayer player) {
        var suit = player.getInventory().getItem(CHEST);
        if (!Gear.is(suit)) {
            return;
        }
        Gear.takeOverflow(player, suit);
        player.openMenu(new SimpleMenuProvider((id, inventory, who) -> new GearGridMenu(id, inventory), suit.getHoverName()));
    }

    private void addInventorySlot(int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y));
    }

    /** The menu button a click on that cell of the grid sends. */
    public static int cellButton(int x, int y) {
        return y * CELL_STRIDE + x;
    }

    public ItemStack suit() {
        return inventory.getItem(CHEST);
    }

    public int gridWidth() {
        return gridWidth;
    }

    public int gridHeight() {
        return gridHeight;
    }

    public int inventoryTop() {
        return inventoryTop;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        var suit = suit();
        if (id < 0 || !Gear.is(suit)) {
            return false;
        }
        int x = id % CELL_STRIDE;
        int y = id / CELL_STRIDE;
        var grid = Gear.grid(suit);
        var carried = getCarried();
        if (carried.isEmpty()) {
            var picked = grid.at(x, y);
            if (picked.isEmpty()) {
                return false;
            }
            Gear.setGrid(suit, grid.remove(picked.get()));
            setCarried(picked.get().item().create());
            return true;
        }
        var upgrade = GearUpgrade.of(carried.typeHolder());
        if (upgrade == null) {
            return false;
        }
        var placed = grid.place(x, y, upgrade.width(), upgrade.height(), ItemStackTemplate.fromNonEmptyStack(carried.copyWithCount(1)));
        if (placed.isEmpty()) {
            return false;
        }
        Gear.setGrid(suit, placed.get());
        carried.shrink(1);
        return true;
    }

    /** A shift-click puts one upgrade in the first rectangle it fits. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        var stack = slot.getItem();
        var suit = suit();
        var upgrade = stack.isEmpty() ? null : GearUpgrade.of(stack.typeHolder());
        if (upgrade == null || !Gear.is(suit) || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        var placed = Gear.grid(suit).placeFirstFit(upgrade.width(), upgrade.height(), ItemStackTemplate.fromNonEmptyStack(stack.copyWithCount(1)));
        if (placed.isPresent()) {
            Gear.setGrid(suit, placed.get());
            stack.shrink(1);
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }

    /** Closing the grid settles the layout, keeping no more FE than its batteries hold. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        var suit = suit();
        if (!player.level().isClientSide() && Gear.is(suit)) {
            Gear.keepWhatBatteriesHold(suit);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Gear.is(suit());
    }

    /** The suit's own slot: shown, and neither taken from nor put into while its grid is open. */
    private static final class Locked extends Slot {

        Locked(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
