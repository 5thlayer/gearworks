// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import java.util.Optional;

import io.github._5thlayer.gearworks.bench.BenchTarget;
import io.github._5thlayer.gearworks.bench.GearBenchMenu;
import io.github._5thlayer.gearworks.gear.Gear;
import io.github._5thlayer.gearworks.gear.GearGridMenu;
import io.github._5thlayer.gearworks.gear.LevelCost;
import io.github._5thlayer.gearworks.grid.GearGrid;
import io.github._5thlayer.gearworks.grid.Placement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * The Gear Bench: the suit's grid, where a click picks the upgrade to raise, and beside it the next
 * Level's cost, each material by the slot it is stocked in with how much of it is stocked, then the FE.
 * The Raise button sends the click; the server raises the Level only once all of the cost is stocked.
 */
public final class GearBenchScreen extends AbstractContainerScreen<GearBenchMenu> {

    private static final int SELECTED = 0xFFFFFFA0;
    private static final int GHOST = 0x808B8B8B;
    private static final int SHORT = 0xFFAA2020;
    private static final int STOCKED = 0xFF208020;
    private static final int RAISE_TOP = GearBenchMenu.STOCK_TOP + LevelCost.MAX_MATERIALS * GearBenchMenu.STOCK_STEP + 14;

    private Button raise;

    public GearBenchScreen(GearBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GearBenchMenu.WIDTH, GearBenchMenu.INVENTORY_TOP + 3 * GearGridMenu.CELL + 4 + GearGridMenu.CELL + 6);
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelX = GearBenchMenu.INVENTORY_LEFT;
        inventoryLabelY = GearBenchMenu.INVENTORY_TOP - 11;
        raise = addRenderableWidget(Button.builder(Component.translatable("gui.gearworks.gear_bench.raise"), button -> send(GearBenchMenu.RAISE))
                .bounds(leftPos + GearBenchMenu.STOCK_LEFT, topPos + RAISE_TOP, 80, 16)
                .build());
        raise.active = menu.ready();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        raise.active = menu.ready();
    }

    private GearGrid<ItemStackTemplate> grid() {
        return Gear.grid(menu.suit());
    }

    /** A cell's size on screen: a vanilla slot's, shrunk so a grid wider than five fits the same square. */
    private int cell() {
        var grid = grid();
        return Math.min(GearGridMenu.CELL, GearBenchMenu.GRID_AREA / Math.max(1, Math.max(grid.width(), grid.height())));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        GridPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            GridPainter.sunk(graphics, leftPos + slot.x, topPos + slot.y, 16, 16);
        }
        int cell = cell();
        int left = leftPos + GearBenchMenu.GRID_LEFT;
        int top = topPos + GearBenchMenu.GRID_TOP;
        GridPainter.grid(graphics, grid(), left, top, cell);
        var target = menu.target();
        target.ifPresent(selected -> {
            var placement = selected.placement();
            graphics.outline(left + placement.x() * cell - 1, top + placement.y() * cell - 1,
                    placement.width() * cell, placement.height() * cell, SELECTED);
        });
        target.map(BenchTarget::cost).ifPresent(cost -> {
            for (int i = 0; i < cost.materials().size(); i++) {
                var slot = menu.slots.get(GearBenchMenu.FIRST_STOCK_SLOT + i);
                if (!slot.hasItem()) {
                    // What the slot takes, greyed out until it holds some.
                    int x = leftPos + slot.x;
                    int y = topPos + slot.y;
                    graphics.item(example(cost.materials().get(i).what()), x, y);
                    graphics.fill(x, y, x + 16, y + 16, GHOST);
                }
            }
        });
    }

    /** The first item a material's tag holds, to show what its slot takes. */
    private static ItemStack example(TagKey<Item> tag) {
        for (var item : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            return new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        var target = menu.target();
        graphics.text(font, heading(target), titleLabelX, titleLabelY + font.lineHeight + 2, GridPainter.LABEL, false);
        var cost = target.map(BenchTarget::cost).orElse(null);
        if (cost == null) {
            return;
        }
        for (int i = 0; i < cost.materials().size(); i++) {
            long stocked = menu.stocked(i);
            long need = cost.materials().get(i).count();
            graphics.text(font, Component.translatable("gui.gearworks.gear_bench.stocked", count(stocked), count(need)),
                    GearBenchMenu.STOCK_LEFT + 20, GearBenchMenu.STOCK_TOP + i * GearBenchMenu.STOCK_STEP + 4,
                    stocked >= need ? STOCKED : SHORT, false);
        }
        long energy = menu.energy();
        graphics.text(font, Component.translatable("gui.gearworks.gear_bench.energy", count(energy), count(cost.energy())),
                GearBenchMenu.STOCK_LEFT, GearBenchMenu.STOCK_TOP + LevelCost.MAX_MATERIALS * GearBenchMenu.STOCK_STEP + 2,
                energy >= cost.energy() ? STOCKED : SHORT, false);
    }

    private static Component heading(Optional<BenchTarget> target) {
        if (target.isEmpty()) {
            return Component.translatable("gui.gearworks.gear_bench.pick");
        }
        var name = target.get().placement().item().create().getHoverName();
        int level = target.get().level();
        return target.get().cost() == null
                ? Component.translatable("gui.gearworks.gear_bench.not_raised", name, level)
                : Component.translatable("gui.gearworks.gear_bench.next", name, level, level + 1);
    }

    private static String count(long value) {
        return FeFormat.format(value, FeFormat.locale(Minecraft.getInstance().getLanguageManager().getSelected()));
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
            send(GearGridMenu.cellButton(cell.get()[0], cell.get()[1]));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void send(int button) {
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    private Optional<Placement<ItemStackTemplate>> placementAt(double mouseX, double mouseY) {
        return cellAt(mouseX, mouseY).flatMap(cell -> grid().at(cell[0], cell[1]));
    }

    private Optional<int[]> cellAt(double mouseX, double mouseY) {
        var grid = grid();
        int cell = cell();
        int x = (int) Math.floor((mouseX - leftPos - GearBenchMenu.GRID_LEFT + 1) / cell);
        int y = (int) Math.floor((mouseY - topPos - GearBenchMenu.GRID_TOP + 1) / cell);
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return Optional.empty();
        }
        return Optional.of(new int[] {x, y});
    }
}
