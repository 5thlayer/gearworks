// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.gear;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github._5thlayer.gearworks.Gearworks;
import io.github._5thlayer.gearworks.level.CostCurve;
import io.github._5thlayer.gearworks.level.CostCurve.Material;
import io.github._5thlayer.gearworks.level.CostCurve.Stage;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jspecify.annotations.Nullable;

/**
 * What raising a Gear Upgrade's Level at a Gear Bench costs, as the {@code gearworks:level_cost} item
 * data map gives it for each upgrade: {@code energy} FE and the tagged materials of the stage the Level
 * falls in, each times {@code growth} per Level ({@link CostCurve}). An upgrade with no entry is never
 * raised. A pack retunes every number, or a Consumer's upgrade gets a cost, with an entry and no code.
 */
public record LevelCost(CostCurve<TagKey<Item>> curve) {

    /** The most materials one stage asks for: the Gear Bench stocks one per slot. */
    public static final int MAX_MATERIALS = 4;

    private static final Codec<Material<TagKey<Item>>> MATERIAL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(Material::what),
            ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(Material::count)
    ).apply(instance, Material::new));

    private static final Codec<Stage<TagKey<Item>>> STAGE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("from").forGetter(Stage::from),
            ExtraCodecs.nonEmptyList(MATERIAL_CODEC.sizeLimitedListOf(MAX_MATERIALS)).fieldOf("materials").forGetter(Stage::materials)
    ).apply(instance, Stage::new));

    private record Raw(long energy, double growth, List<Stage<TagKey<Item>>> stages) {
    }

    private static final Codec<Raw> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_LONG.fieldOf("energy").forGetter(Raw::energy),
            Codec.doubleRange(1, Double.MAX_VALUE).fieldOf("growth").forGetter(Raw::growth),
            ExtraCodecs.nonEmptyList(STAGE_CODEC.listOf()).fieldOf("stages").forGetter(Raw::stages)
    ).apply(instance, Raw::new));

    public static final Codec<LevelCost> CODEC = RAW_CODEC.flatXmap(LevelCost::of,
            cost -> DataResult.success(new Raw(cost.curve.energy(), cost.curve.growth(), cost.curve.stages())));

    /** Synced, since the Gear Bench's screen shows the next Level's cost. */
    public static final DataMapType<Item, LevelCost> DATA_MAP = DataMapType.builder(
            Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "level_cost"), Registries.ITEM, CODEC)
            .synced(CODEC, false)
            .build();

    private static DataResult<LevelCost> of(Raw raw) {
        try {
            return DataResult.success(new LevelCost(new CostCurve<>(raw.energy, raw.growth, raw.stages)));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e::getMessage);
        }
    }

    /** That upgrade's cost, or null when a Gear Bench never raises it. */
    public static @Nullable LevelCost of(Holder<Item> upgrade) {
        return upgrade.getData(DATA_MAP);
    }

    /** What raising an upgrade at that Level to the next costs. */
    public CostCurve.Cost<TagKey<Item>> at(int level) {
        return curve.at(level);
    }
}
