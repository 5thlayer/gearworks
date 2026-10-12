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
 * click on a cell asks the server to place or pick up there; the server answers by syncing the suit.
 */
public final class GearGridScreen extends AbstractContainerScreen<GearGridMenu> {

    private static final int CELL = GearGridMenu.CELL;
    // Vanilla's container colours.
    private static final int PANEL = 0xFFC6C6C6;
    private static final int EDGE = 0xFF000000;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SHADE = 0xFF555555;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_SHADE = 0xFF373737;
    private static final int LABEL = 0xFF404040;
    private static final int UPGRADE = 0xFF6F7F8F;
    private static final int UPGRADE_EDGE = 0xFF2F3F4F;
    private static final int FITS = 0x6000FF00;
    private static final int REFUSED = 0x60FF0000;

    public GearGridScreen(GearGridMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, width(menu), menu.inventoryTop() + 3 * CELL + 4 + CELL + 6);
    }

    private static int width(GearGridMenu menu) {
        return Math.max(GearGridMenu.GRID_LEFT + menu.gridWidth() * CELL + 8, menu.inventoryLeft() * 2 + 9 * CELL - 2);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = GearGridMenu.GRID_LEFT;
        titleLabelY = 6;
        inventoryLabelX = menu.inventoryLeft();
        inventoryLabelY = menu.inventoryTop() - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, EDGE);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, LIGHT);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 1, topPos + imageHeight - 1, SHADE);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, PANEL);
        for (var slot : menu.slots) {
            sunk(graphics, leftPos + slot.x, topPos + slot.y, 16, 16);
        }
        var grid = Gear.grid(menu.suit());
        for (int y = 0; y < menu.gridHeight(); y++) {
            for (int x = 0; x < menu.gridWidth(); x++) {
                sunk(graphics, cellX(x), cellY(y), 16, 16);
            }
        }
        for (var placement : grid.placements()) {
            int x0 = cellX(placement.x());
            int y0 = cellY(placement.y());
            int w = placement.width() * CELL - 2;
            int h = placement.height() * CELL - 2;
            graphics.fill(x0, y0, x0 + w, y0 + h, UPGRADE_EDGE);
            graphics.fill(x0 + 1, y0 + 1, x0 + w - 1, y0 + h - 1, UPGRADE);
            graphics.item(placement.item().create(), x0 + (w - 16) / 2, y0 + (h - 16) / 2);
        }
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

    private static void sunk(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, SLOT_SHADE);
        graphics.fill(x, y, x + w + 1, y + h + 1, LIGHT);
        graphics.fill(x, y, x + w, y + h, SLOT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        var suit = menu.suit();
        var energy = Component.translatable("tooltip.gearworks.energy", Gear.energy(suit), Gear.capacity(suit));
        graphics.text(font, energy, titleLabelX, titleLabelY + font.lineHeight + 2, LABEL, false);
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
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, GearGridMenu.cellButton(cell.get()[0], cell.get()[1]));
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
