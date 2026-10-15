// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github._5thlayer.gearworks.bench.BenchTarget;
import io.github._5thlayer.gearworks.bench.GearBenchMenu;
import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.gear.GearGridMenu;
import io.github._5thlayer.gearworks.gear.LevelCost;
import io.github._5thlayer.gearworks.grid.GearGrid;
import io.github._5thlayer.gearworks.grid.Placement;
import io.github._5thlayer.gearworks.level.CostCurve;
import io.github._5thlayer.libworks.ghost.Ghosts;
import io.github._5thlayer.libworks.ghost.client.GhostDraw;
import io.github._5thlayer.libworks.machine.client.MachineDraw;
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
 * The Gear Bench: the suit's grid at full size, where a click picks the upgrade to raise, and under it
 * the next Level's cost: a slot per material, which shows what it takes as a libworks ghost at its
 * count while empty, and a bar of the FE stocked against the FE it costs. The empty suit slot ghosts
 * any suit. The Raise button sends the click; the server raises the Level only once all of the cost is
 * stocked.
 */
public final class GearBenchScreen extends AbstractContainerScreen<GearBenchMenu> {

    private static final int SELECTED = 0xFFFFFFA0;
    /** The FE bars' red, as the other -works machines draw theirs. */
    private static final int ENERGY = 0xFFB8392B;
    private static final int CELL = GearGridMenu.CELL;
    private static final int BAR_WIDTH = GearBenchMenu.WIDTH - 2 * GearBenchMenu.STOCK_LEFT;
    // The room a bar keeps either side of its label, as MachineDraw.bar does.
    private static final int LABEL_PADDING = 4;
    private static final int RAISE_WIDTH = 56;

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
        titleLabelX = GearGridMenu.LABEL_LEFT;
        titleLabelY = 7;
        inventoryLabelX = GearBenchMenu.INVENTORY_LEFT;
        inventoryLabelY = GearBenchMenu.INVENTORY_TOP - 11;
        raise = addRenderableWidget(Button.builder(Component.translatable("gui.gearworks.gear_bench.raise"), button -> send(GearBenchMenu.RAISE))
                .bounds(leftPos + GearBenchMenu.WIDTH - GearBenchMenu.STOCK_LEFT - RAISE_WIDTH, topPos + GearBenchMenu.STOCK_TOP, RAISE_WIDTH, 16)
                .build());
        raise.active = menu.ready();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        raise.active = menu.ready();
    }

    private GearGrid<ItemStackTemplate> grid() {
        return Suit.grid(menu.suit());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        GridPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (var slot : menu.slots) {
            GridPainter.sunk(graphics, leftPos + slot.x, topPos + slot.y, 16, 16);
        }
        int left = leftPos + GearBenchMenu.GRID_LEFT;
        int top = topPos + GearBenchMenu.GRID_TOP;
        GridPainter.grid(graphics, grid(), left, top, CELL);
        var target = menu.target();
        target.ifPresent(selected -> {
            var placement = selected.placement();
            graphics.outline(left + placement.x() * CELL - 1, top + placement.y() * CELL - 1,
                    placement.width() * CELL, placement.height() * CELL, SELECTED);
        });
        MachineDraw.bar(graphics, font, leftPos + GearBenchMenu.STOCK_LEFT, topPos + GearBenchMenu.ENERGY_TOP, BAR_WIDTH,
                MachineDraw.BAR_HEIGHT, charge(), ENERGY, energyLine());
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
        var heading = heading(menu.target());
        int width = imageWidth - titleLabelX - 8;
        graphics.text(font, MachineDraw.fitted(font, heading, width), titleLabelX, titleLabelY + font.lineHeight + 2, GridPainter.LABEL, false);
    }

    /** The share of the next Level's FE the bench has stocked; empty with nothing picked to raise. */
    private float charge() {
        long cost = nextCost();
        return cost <= 0 ? 0f : Math.min(1f, (float) menu.energy() / cost);
    }

    /** The FE stocked against what the next Level costs, or the FE alone with nothing picked. */
    private Component energyLine() {
        long cost = nextCost();
        return cost <= 0
                ? Component.translatable("gui.gearworks.gear_bench.energy_held", count(menu.energy()))
                : Component.translatable("gui.gearworks.gear_bench.energy", count(menu.energy()), count(cost));
    }

    private long nextCost() {
        return menu.target().map(BenchTarget::cost).map(CostCurve.Cost::energy).orElse(0L);
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
        if (MachineDraw.over(leftPos + GearBenchMenu.STOCK_LEFT, topPos + GearBenchMenu.ENERGY_TOP, BAR_WIDTH, MachineDraw.BAR_HEIGHT,
                mouseX, mouseY)) {
            MachineDraw.wholeOnHover(graphics, font, energyLine(), BAR_WIDTH - LABEL_PADDING, mouseX, mouseY);
        }
        var heading = heading(menu.target());
        if (MachineDraw.over(leftPos + titleLabelX, topPos + titleLabelY + font.lineHeight + 2, imageWidth - titleLabelX - 8, font.lineHeight,
                mouseX, mouseY)) {
            MachineDraw.wholeOnHover(graphics, font, heading, imageWidth - titleLabelX - 8, mouseX, mouseY);
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
        int x = (int) Math.floor((mouseX - leftPos - GearBenchMenu.GRID_LEFT + 1) / CELL);
        int y = (int) Math.floor((mouseY - topPos - GearBenchMenu.GRID_TOP + 1) / CELL);
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return Optional.empty();
        }
        return Optional.of(new int[] {x, y});
    }
}
