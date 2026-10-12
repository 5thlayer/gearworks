// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.bench;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github._5thlayer.gearworks.gear.LevelCost;
import io.github._5thlayer.gearworks.level.CostCurve.Cost;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.transfer.ResourceStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import org.jspecify.annotations.Nullable;

/**
 * The materials a Gear Bench has stocked toward the next Level: a slot per material of its cost, each
 * taking only that material's tag and only as many as the Level needs, so a hopper never fills the
 * bench with what it cannot spend. A slot holds more than a stack, as a high Level asks for more. What
 * a slot holds after the bench is set to another upgrade stays there, counting for nothing, until a
 * player takes it out.
 */
final class BenchStock extends ResourceStacksResourceHandler<ItemResource> {

    private static final Codec<ResourceStack<ItemResource>> STACK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemResource.OPTIONAL_CODEC.optionalFieldOf("item", ItemResource.EMPTY).forGetter(ResourceStack::resource),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("count", 0).forGetter(ResourceStack::amount)
    ).apply(instance, ResourceStack::new));

    private final Supplier<@Nullable Cost<TagKey<Item>>> cost;
    private final Runnable changed;

    /**
     * @param cost    the next Level's cost, or null while the bench has nothing to raise; null always
     *                for the client's copy, which takes whatever the server syncs into it
     * @param changed called after a slot changes, to save the bench
     */
    BenchStock(Supplier<@Nullable Cost<TagKey<Item>>> cost, Runnable changed) {
        super(LevelCost.MAX_MATERIALS, ItemResource.EMPTY, STACK_CODEC);
        this.cost = cost;
        this.changed = changed;
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        var need = cost.get();
        return need != null && index < need.materials().size() && resource.typeHolder().is(need.materials().get(index).what());
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        var need = cost.get();
        return need == null || index >= need.materials().size() ? 0 : need.materials().get(index).count();
    }

    @Override
    protected void onContentsChanged(int index, ResourceStack<ItemResource> previousContents) {
        changed.run();
    }
}
