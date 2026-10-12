// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import java.util.List;
import java.util.Locale;

import io.github._5thlayer.gearworks.gear.Gear;
import io.github._5thlayer.gearworks.gear.GearUpgrade;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * A suit's FE and the totals of its Stats, not each upgrade's value, since a Stat's Ceiling holds for
 * their sum (ADR 0003); and every Gear Upgrade's size, Level and Stat at that Level, from its data map
 * entry, so an upgrade a datapack adds says so as Gearworks' own do.
 */
final class GearTooltips {

    private GearTooltips() {
    }

    static void onTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        var lines = event.getToolTip();
        if (Gear.is(stack)) {
            suit(stack, lines);
        }
        var upgrade = GearUpgrade.of(stack.typeHolder());
        if (upgrade != null) {
            upgrade(upgrade, GearUpgrade.level(stack), lines);
        }
    }

    private static void suit(ItemStack suit, List<Component> lines) {
        var totals = Gear.totals(suit);
        lines.add(gray(FeText.reading(suit)));
        if (totals.solar() > 0) {
            lines.add(gray(Component.translatable("tooltip.gearworks.solar", format(totals.solar()))));
        }
        if (totals.speed() > 0) {
            var percent = format(totals.speedBonus(GearworksConfig.speedCeiling()) * 100);
            lines.add(gray(Component.translatable("tooltip.gearworks.speed", percent, totals.speedDraw())));
        }
    }

    /** The upgrade's own value at its Level; a Stat with a Ceiling shows the suit's total on the suit. */
    private static void upgrade(GearUpgrade upgrade, int level, List<Component> lines) {
        lines.add(gray(Component.translatable("tooltip.gearworks.upgrade.size", upgrade.width(), upgrade.height())));
        lines.add(gray(Component.translatable("tooltip.gearworks.upgrade.level", level)));
        double amount = upgrade.amountAt(level);
        lines.add(gray(switch (upgrade.stat()) {
            case BATTERY -> Component.translatable("tooltip.gearworks.upgrade.battery", format(amount));
            case SOLAR -> Component.translatable("tooltip.gearworks.upgrade.solar", format(amount));
            case SPEED -> Component.translatable("tooltip.gearworks.upgrade.speed", format(amount * 100), upgrade.drawAt(level));
        }));
    }

    private static Component gray(MutableComponent line) {
        return line.withStyle(ChatFormatting.GRAY);
    }

    private static String format(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
