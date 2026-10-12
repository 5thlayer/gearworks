// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github._5thlayer.gearworks.Gearworks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jspecify.annotations.Nullable;

/**
 * A Gear Upgrade, as the {@code gearworks:gear_upgrade} item data map gives it: any item with an
 * entry fits the Gear Grid, covering {@code width} by {@code height} cells and giving its Stat
 * {@code amount}, while drawing {@code draw} FE per tick when the Stat runs on FE. The data map is the
 * API: a Consumer or a pack adds an upgrade with an entry and no code.
 */
public record GearUpgrade(int width, int height, Stat stat, double amount, int draw) {

    public static final Codec<GearUpgrade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(GearUpgrade::width),
            ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(GearUpgrade::height),
            Stat.CODEC.fieldOf("stat").forGetter(GearUpgrade::stat),
            Codec.doubleRange(0, Double.MAX_VALUE).fieldOf("amount").forGetter(GearUpgrade::amount),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("draw", 0).forGetter(GearUpgrade::draw)
    ).apply(instance, GearUpgrade::new));

    /** Synced, since the client sizes the upgrade it is about to place and shows the suit's totals. */
    public static final DataMapType<Item, GearUpgrade> DATA_MAP = DataMapType.builder(
            Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "gear_upgrade"), Registries.ITEM, CODEC)
            .synced(CODEC, false)
            .build();

    /** The upgrade that item is, or null when it fits no grid. */
    public static @Nullable GearUpgrade of(Holder<Item> item) {
        return item.getData(DATA_MAP);
    }
}
