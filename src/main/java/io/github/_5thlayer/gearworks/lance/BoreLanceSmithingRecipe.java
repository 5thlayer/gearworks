// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.lance;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * A {@code smithing_transform} that makes a Bore Lance from the tier below and keeps what it carried, as
 * vanilla's does, but for what the new tier's {@link BoreLanceItem.Upkeep} has nothing to act on: a tier
 * on FE drops Unbreaking and Mending, and one with no durability drops the wear the old one had.
 */
public final class BoreLanceSmithingRecipe extends SmithingTransformRecipe {

    public static final MapCodec<BoreLanceSmithingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            Ingredient.CODEC.optionalFieldOf("template").forGetter(SmithingTransformRecipe::templateIngredient),
            Ingredient.CODEC.fieldOf("base").forGetter(SmithingTransformRecipe::baseIngredient),
            Ingredient.CODEC.optionalFieldOf("addition").forGetter(SmithingTransformRecipe::additionIngredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.lance)
    ).apply(instance, BoreLanceSmithingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoreLanceSmithingRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
            Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, SmithingTransformRecipe::templateIngredient,
            Ingredient.CONTENTS_STREAM_CODEC, SmithingTransformRecipe::baseIngredient,
            Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, SmithingTransformRecipe::additionIngredient,
            ItemStackTemplate.STREAM_CODEC, recipe -> recipe.lance,
            BoreLanceSmithingRecipe::new);

    public static final RecipeSerializer<BoreLanceSmithingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate lance;

    public BoreLanceSmithingRecipe(Recipe.CommonInfo commonInfo, Optional<Ingredient> template, Ingredient base,
            Optional<Ingredient> addition, ItemStackTemplate lance) {
        super(commonInfo, template, base, addition, lance);
        this.lance = lance;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        var made = super.assemble(input);
        if (made.getItem() instanceof BoreLanceItem tier) {
            EnchantmentHelper.updateEnchantments(made, enchantments -> enchantments.removeIf(enchantment -> !tier.upkeep().takes(enchantment)));
        }
        if (!made.isDamageableItem()) {
            made.remove(DataComponents.DAMAGE);
        }
        return made;
    }

    @Override
    public RecipeSerializer<SmithingTransformRecipe> getSerializer() {
        @SuppressWarnings("unchecked")
        var serializer = (RecipeSerializer<SmithingTransformRecipe>) (RecipeSerializer<?>) SERIALIZER;
        return serializer;
    }
}
