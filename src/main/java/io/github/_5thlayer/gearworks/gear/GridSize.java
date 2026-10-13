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

/**
 * The size of a tier's Gear Grid, as the {@code gearworks:gear_grid} item data map gives it for each
 * suit item, so a pack can resize a tier with no code. A suit with no entry has no grid. A grid is at
 * most nine columns, the inventory's, so its cells stand over the inventory's slots, and six rows, the
 * room the Gear Bench keeps for it; the tiers differ in rows.
 */
public record GridSize(int width, int height) {

    public static final int MAX_WIDTH = 9;
    public static final int MAX_HEIGHT = 6;

    public static final GridSize NONE = new GridSize(0, 0);

    public static final Codec<GridSize> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.intRange(1, MAX_WIDTH).fieldOf("width").forGetter(GridSize::width),
            ExtraCodecs.intRange(1, MAX_HEIGHT).fieldOf("height").forGetter(GridSize::height)
    ).apply(instance, GridSize::new));

    /** Synced, since the client draws the grid. */
    public static final DataMapType<Item, GridSize> DATA_MAP = DataMapType.builder(
            Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "gear_grid"), Registries.ITEM, CODEC)
            .synced(CODEC, false)
            .build();

    public static GridSize of(Holder<Item> suit) {
        var size = suit.getData(DATA_MAP);
        return size == null ? NONE : size;
    }
}
