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
import io.github._5thlayer.gearworks.level.Curves;
import io.github._5thlayer.gearworks.railgun.Railgun;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * A suit's FE and the totals of its Stats, not each upgrade's value, since a Stat's Ceiling holds for
 * their sum (ADR 0003); every Gear Upgrade's size, Level and Stat at that Level, from its data map
 * entry, so an upgrade a datapack adds says so as Gearworks' own do; and a Bore Lance's Bore and its
 * built-in Lunge, which vanilla's enchantment lines don't show, and the netherite lance's FE; and a
 * Railgun's FE, and whether it is Charged or too dry to load its next shot.
 */
final class GearTooltips {

    /** The Stats that make FE, each shown on the suit as the FE its upgrades make together. */
    private static final List<Stat> GENERATORS = List.of(Stat.SOLAR, Stat.NUCLEAR, Stat.KINETIC, Stat.COMBUSTION, Stat.METABOLIC);

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
        if (Railgun.is(stack)) {
            railgun(stack, lines);
        }
        var upgrade = GearUpgrade.of(stack.typeHolder());
        if (upgrade != null) {
            upgrade(upgrade, GearUpgrade.level(stack), lines);
        }
    }

    private static void suit(ItemStack suit, List<Component> lines) {
        var totals = Suit.totals(suit);
        lines.add(gray(FeText.reading(suit)));
        for (var stat : GENERATORS) {
            if (totals.amount(stat) > 0) {
                lines.add(gray(Component.translatable("tooltip.gearworks." + stat.getSerializedName(), format(totals.amount(stat)))));
            }
        }
        for (var stat : List.of(Stat.SPEED, Stat.SHIELD, Stat.JUMP)) {
            if (totals.has(stat)) {
                lines.add(gray(Component.translatable("tooltip.gearworks." + stat.getSerializedName(),
                        format(totals.underCeiling(stat) * 100), totals.draw(stat))));
            }
        }
        if (totals.has(Stat.REACH)) {
            lines.add(gray(Component.translatable("tooltip.gearworks.reach", format(totals.underCeiling(Stat.REACH)), totals.draw(Stat.REACH))));
        }
        if (totals.has(Stat.NIGHT_VISION)) {
            lines.add(gray(Component.translatable("tooltip.gearworks.night_vision", totals.draw(Stat.NIGHT_VISION))));
        }
        if (totals.has(Stat.FLIGHT)) {
            lines.add(gray(Component.translatable("tooltip.gearworks.flight",
                    format(totals.underCeiling(Stat.FLIGHT) * 100), totals.draw(Stat.FLIGHT))));
        }
        if (totals.has(Stat.BOOST)) {
            lines.add(gray(Component.translatable("tooltip.gearworks.boost",
                    format(totals.underCeiling(Stat.BOOST)), totals.draw(Stat.BOOST), seconds(totals.cooldown(Stat.BOOST)))));
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

    private static void railgun(ItemStack railgun, List<Component> lines) {
        lines.add(gray(FeText.reading(Railgun.energy(railgun), GearworksConfig.railgunCapacity())));
        if (Railgun.charged(railgun)) {
            lines.add(Component.translatable("tooltip.gearworks.railgun.charged").withStyle(ChatFormatting.AQUA));
        } else if (Railgun.energy(railgun) < GearworksConfig.railgunChargeEnergy()) {
            lines.add(Component.translatable("tooltip.gearworks.railgun.dry").withStyle(ChatFormatting.RED));
        }
    }

    /** The upgrade's own value at its Level; a Stat with a Ceiling shows the suit's total on the suit. */
    private static void upgrade(GearUpgrade upgrade, int level, List<Component> lines) {
        lines.add(gray(Component.translatable("tooltip.gearworks.upgrade.size", upgrade.width(), upgrade.height())));
        lines.add(gray(Component.translatable("tooltip.gearworks.upgrade.level", level)));
        double amount = upgrade.amountAt(level);
        lines.add(gray(switch (upgrade.stat()) {
            case BATTERY -> Component.translatable("tooltip.gearworks.upgrade.battery", format(amount));
            case SOLAR, NUCLEAR, KINETIC, COMBUSTION, METABOLIC ->
                    Component.translatable("tooltip.gearworks.upgrade." + upgrade.stat().getSerializedName(), format(amount));
            case SPEED -> Component.translatable("tooltip.gearworks.upgrade.speed", format(amount * 100), upgrade.drawAt(level));
            case SHIELD -> Component.translatable("tooltip.gearworks.upgrade.shield", format(amount * 100), upgrade.drawAt(level));
            case JUMP -> Component.translatable("tooltip.gearworks.upgrade.jump", format(amount * 100), upgrade.drawAt(level));
            case REACH -> Component.translatable("tooltip.gearworks.upgrade.reach", format(amount), upgrade.drawAt(level));
            case NIGHT_VISION -> Component.translatable("tooltip.gearworks.upgrade.night_vision", upgrade.drawAt(level));
            // One to a suit, so what it gives under the Ceiling is the suit's total too.
            case FLIGHT -> Component.translatable("tooltip.gearworks.upgrade.flight",
                    format(Curves.underCeiling(amount, GearworksConfig.ceiling(Stat.FLIGHT)) * 100), upgrade.drawAt(level));
            // One to a suit, as flight is.
            case BOOST -> Component.translatable("tooltip.gearworks.upgrade.boost",
                    format(Curves.underCeiling(amount, GearworksConfig.ceiling(Stat.BOOST))), upgrade.drawAt(level),
                    seconds(upgrade.cooldownAt(level)));
        }));
    }

    private static Component gray(MutableComponent line) {
        return line.withStyle(ChatFormatting.GRAY);
    }

    private static String seconds(int ticks) {
        return format(ticks / 20.0);
    }

    private static String format(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }
}
