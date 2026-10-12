// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github._5thlayer.gearworks.bench.BenchTarget;
import io.github._5thlayer.gearworks.bench.GearBenchMenu;
import io.github._5thlayer.gearworks.gear.Gear;
import io.github._5thlayer.gearworks.gear.GearGridMenu;
import io.github._5thlayer.gearworks.gear.LevelCost;
import io.github._5thlayer.gearworks.grid.GearGrid;
import io.github._5thlayer.gearworks.grid.Placement;
import io.github._5thlayer.libworks.ghost.Ghosts;
import io.github._5thlayer.libworks.ghost.client.GhostDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * The Gear Bench: the suit's grid, where a click picks the upgrade to raise, and beside it the next
 * Level's cost, each material by the slot it is stocked in with how much of it is stocked, then the FE.
 * An empty slot shows what it takes as a libworks ghost: any suit, or the material at its count.
 * The Raise button sends the click; the server raises the Level only once all of the cost is stocked.
 */
public final class GearBenchScreen extends AbstractContainerScreen<GearBenchMenu> {

    private static final int SELECTED = 0xFFFFFFA0;
    private static final int SHORT = 0xFFAA2020;
    private static final int STOCKED = 0xFF208020;
    private static final int RAISE_TOP = GearBenchMenu.STOCK_TOP + LevelCost.MAX_MATERIALS * GearBenchMenu.STOCK_STEP + 14;

    /** What the empty suit slot and each empty material slot take, faded. */
    private final Ghosts ghosts = new Ghosts(() -> leftPos, () -> topPos);
    private Button raise;

    public GearBenchScreen(GearBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GearBenchMenu.WIDTH, GearBenchMenu.INVENTORY_TOP + 3 * GearGridMenu.CELL + 4 + GearGridMenu.CELL + 6);
        ghosts.add(menu.slots.get(GearBenchMenu.SUIT_SLOT));
        for (int i = 0; i < LevelCost.MAX_MATERIALS; i++) {
            int index = i;
            ghosts.add(menu.slots.get(GearBenchMenu.FIRST_STOCK_SLOT + i), () -> material(index));
        }
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
        for (var ghost : ghosts.shown(Util.getMillis())) {
            GhostDraw.draw(graphics, font, ghost);
        }
    }

    /** The members of the material's tag at that index of the next Level's cost, each at its count. */
    private List<ItemStack> material(int index) {
        var cost = menu.target().map(BenchTarget::cost).orElse(null);
        if (cost == null || index >= cost.materials().size()) {
            return List.of();
        }
        var material = cost.materials().get(index);
        var members = new ArrayList<ItemStack>();
        for (var item : BuiltInRegistries.ITEM.getTagOrEmpty(material.what())) {
            members.add(new ItemStack(item, material.count()));
        }
        return members;
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
        if (!menu.getCarried().isEmpty() || hoveredSlot != null && hoveredSlot.hasItem()) {
            return;
        }
        ghosts.at(mouseX, mouseY, Util.getMillis()).ifPresent(ghost -> GhostDraw.tooltip(graphics, font, ghost, mouseX, mouseY));
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
