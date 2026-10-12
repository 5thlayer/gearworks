// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github._5thlayer.gearworks.grid.Placement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * The {@code gearworks:gear_grid} component: the upgrades a suit's Gear Grid holds and where, saved
 * with the suit so the layout survives a save and reload and travels with the item.
 */
public record GridContents(List<Placement<ItemStackTemplate>> placements) {

    public static final GridContents EMPTY = new GridContents(List.of());

    private static final Codec<Placement<ItemStackTemplate>> PLACEMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("x").forGetter(Placement::x),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("y").forGetter(Placement::y),
            ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(Placement::width),
            ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(Placement::height),
            ItemStackTemplate.CODEC.fieldOf("item").forGetter(Placement::item)
    ).apply(instance, Placement::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, Placement<ItemStackTemplate>> PLACEMENT_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Placement::x,
            ByteBufCodecs.VAR_INT, Placement::y,
            ByteBufCodecs.VAR_INT, Placement::width,
            ByteBufCodecs.VAR_INT, Placement::height,
            ItemStackTemplate.STREAM_CODEC, Placement::item,
            Placement::new);

    public static final Codec<GridContents> CODEC = PLACEMENT_CODEC.listOf().xmap(GridContents::new, GridContents::placements);

    public static final StreamCodec<RegistryFriendlyByteBuf, GridContents> STREAM_CODEC =
            PLACEMENT_STREAM_CODEC.apply(ByteBufCodecs.list()).map(GridContents::new, GridContents::placements);

    public GridContents {
        placements = List.copyOf(placements);
    }
}
