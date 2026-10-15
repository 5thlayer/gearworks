// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import java.util.List;
import java.util.Locale;

import io.github._5thlayer.gearworks.gear.Suit;
import io.github._5thlayer.gearworks.gear.GearUpgrade;
import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.gear.Stat;
import io.github._5thlayer.gearworks.lance.BoreLance;
import io.github._5thlayer.gearworks.lance.BoreLanceItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * A suit's FE and the totals of its Stats, not each upgrade's value, since a Stat's Ceiling holds for
 * their sum (ADR 0003); every Gear Upgrade's size, Level and Stat at that Level, from its data map
 * entry, so an upgrade a datapack adds says so as Gearworks' own do; and a Bore Lance's Bore and its
 * built-in Lunge, which vanilla's enchantment lines don't show, and the netherite lance's FE.
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
        if (Suit.is(stack)) {
            suit(stack, lines);
        }
        if (BoreLance.is(stack)) {
            lance(stack, (BoreLanceItem) stack.getItem(), lines);
        }
        var upgrade = GearUpgrade.of(stack.typeHolder());
        if (upgrade != null) {
            upgrade(upgrade, GearUpgrade.level(stack), lines);
        }
    }

    private static void suit(ItemStack suit, List<Component> lines) {
        var totals = Suit.totals(suit);
        lines.add(gray(FeText.reading(suit)));
        if (totals.amount(Stat.SOLAR) > 0) {
            lines.add(gray(Component.translatable("tooltip.gearworks.solar", format(totals.amount(Stat.SOLAR)))));
        }
        for (var stat : List.of(Stat.SPEED, Stat.SHIELD, Stat.JUMP)) {
            if (totals.has(stat)) {
                lines.add(gray(Component.translatable("tooltip.gearworks." + stat.getSerializedName(),
                        format(totals.underCeiling(stat) * 100), totals.draw(stat))));
            }
        }
        if (totals.has(Stat.NIGHT_VISION)) {
            lines.add(gray(Component.translatable("tooltip.gearworks.night_vision", totals.draw(Stat.NIGHT_VISION))));
        }
        if (totals.has(Stat.FLIGHT)) {
            lines.add(gray(Component.translatable("tooltip.gearworks.flight",
                    format(totals.underCeiling(Stat.FLIGHT) * 100), totals.draw(Stat.FLIGHT))));
        }
    }

    /**
     * The Bore the lance breaks, its built-in Lunge, whether it is worn out, and on FE, its FE and whether it
     * is dry, or too low to lunge.
     */
    private static void lance(ItemStack stack, BoreLanceItem lance, List<Component> lines) {
        if (BoreLance.runsOnEnergy(stack)) {
            lines.add(gray(FeText.reading(BoreLance.energy(stack), GearworksConfig.netheriteLanceCapacity())));
        }
        lines.add(gray(Component.translatable("tooltip.gearworks.bore_lance.bore", BoreLance.bore(stack).size())));
        lines.add(gray(Component.translatable("tooltip.gearworks.bore_lance.lunge",
                Component.translatable("enchantment.level." + lance.lunge()))));
        if (BoreLance.wornOut(stack)) {
            lines.add(Component.translatable("tooltip.gearworks.bore_lance.worn_out").withStyle(ChatFormatting.RED));
        }
        if (BoreLance.dry(stack)) {
            lines.add(Component.translatable("tooltip.gearworks.bore_lance.dry").withStyle(ChatFormatting.RED));
        } else if (BoreLance.runsOnEnergy(stack) && !BoreLance.affordsLunge(stack)) {
            lines.add(Component.translatable("tooltip.gearworks.bore_lance.low").withStyle(ChatFormatting.RED));
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
            case SHIELD -> Component.translatable("tooltip.gearworks.upgrade.shield", format(amount * 100), upgrade.drawAt(level));
            case JUMP -> Component.translatable("tooltip.gearworks.upgrade.jump", format(amount * 100), upgrade.drawAt(level));
            case NIGHT_VISION -> Component.translatable("tooltip.gearworks.upgrade.night_vision", upgrade.drawAt(level));
            case FLIGHT -> Component.translatable("tooltip.gearworks.upgrade.flight", format(amount * 100), upgrade.drawAt(level));
        }));
    }

    private static Component gray(MutableComponent line) {
        return line.withStyle(ChatFormatting.GRAY);
    }

    private static String format(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
