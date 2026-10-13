// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import java.util.Optional;

import io.github._5thlayer.gearworks.gear.Gear;
import io.github._5thlayer.gearworks.gear.GearGridMenu;
import io.github._5thlayer.gearworks.gear.GearUpgrade;
import io.github._5thlayer.gearworks.grid.Placement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * The Gear Grid: its cells, the upgrades placed over them, and where the carried upgrade would go. A
 * click on a cell asks the server to place, pick up or stack there, and a shift-click to send the
 * upgrade to the inventory; the server answers by syncing the suit.
 */
public final class GearGridScreen extends AbstractContainerScreen<GearGridMenu> {

    private static final int CELL = GearGridMenu.CELL;
    private static final int FITS = 0x6000FF00;
    private static final int REFUSED = 0x60FF0000;

    public GearGridScreen(GearGridMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GearGridMenu.WIDTH, menu.inventoryTop() + 3 * CELL + 4 + CELL + 6);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = GearGridMenu.LABEL_LEFT;
        titleLabelY = 7;
        inventoryLabelX = GearGridMenu.INVENTORY_LEFT;
        inventoryLabelY = menu.inventoryTop() - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        GridPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            GridPainter.sunk(graphics, leftPos + slot.x, topPos + slot.y, 16, 16);
        }
        GridPainter.grid(graphics, Gear.grid(menu.suit()), cellX(0), cellY(0), CELL);
        preview(graphics, mouseX, mouseY);
    }

    /** Where the carried upgrade would go with its top-left on the hovered cell, green if it fits. */
    private void preview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        var carried = menu.getCarried();
        var cell = cellAt(mouseX, mouseY);
        var upgrade = carried.isEmpty() ? null : GearUpgrade.of(carried.typeHolder());
        if (upgrade == null || cell.isEmpty()) {
            return;
        }
        int x = cell.get()[0];
        int y = cell.get()[1];
        boolean fits = Gear.grid(menu.suit()).fits(x, y, upgrade.width(), upgrade.height());
        int right = Math.min(x + upgrade.width(), menu.gridWidth());
        int bottom = Math.min(y + upgrade.height(), menu.gridHeight());
        graphics.fill(cellX(x), cellY(y), cellX(right) - 2, cellY(bottom) - 2, fits ? FITS : REFUSED);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, FeText.reading(menu.suit()), titleLabelX, titleLabelY + font.lineHeight + 2, GridPainter.LABEL, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        placementAt(mouseX, mouseY).ifPresent(placement -> {
            var stack = placement.item().create();
            graphics.setTooltipForNextFrame(font, getTooltipFromContainerItem(stack), stack.getTooltipImage(), stack, mouseX, mouseY);
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        var cell = cellAt(event.x(), event.y());
        if (cell.isPresent() && event.button() == 0) {
            if (minecraft.gameMode != null) {
                int x = cell.get()[0];
                int y = cell.get()[1];
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                        event.hasShiftDown() ? GearGridMenu.shiftCellButton(x, y) : GearGridMenu.cellButton(x, y));
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private Optional<Placement<ItemStackTemplate>> placementAt(double mouseX, double mouseY) {
        return cellAt(mouseX, mouseY).flatMap(cell -> Gear.grid(menu.suit()).at(cell[0], cell[1]));
    }

    private Optional<int[]> cellAt(double mouseX, double mouseY) {
        int x = (int) Math.floor((mouseX - cellX(0) + 1) / CELL);
        int y = (int) Math.floor((mouseY - cellY(0) + 1) / CELL);
        if (x < 0 || y < 0 || x >= menu.gridWidth() || y >= menu.gridHeight()) {
            return Optional.empty();
        }
        return Optional.of(new int[] {x, y});
    }

    private int cellX(int x) {
        return leftPos + GearGridMenu.GRID_LEFT + x * CELL;
    }

    private int cellY(int y) {
        return topPos + GearGridMenu.GRID_TOP + y * CELL;
    }
}
