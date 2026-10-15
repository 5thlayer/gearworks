// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.lance;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.TransmuteRecipe;

/**
 * A shaped recipe that makes a Bore Lance from a spear and keeps what the spear carried, its
 * enchantments and its wear, as {@code crafting_transmute} does; the spear's own Lunge gives way to the
 * lance's built-in one.
 */
public final class BoreLanceRecipe extends ShapedRecipe {

    public static final MapCodec<BoreLanceRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(recipe -> recipe.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(recipe -> recipe.lance)
    ).apply(instance, BoreLanceRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoreLanceRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, recipe -> recipe.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.pattern,
            ItemStackTemplate.STREAM_CODEC, recipe -> recipe.lance,
            BoreLanceRecipe::new);

    public static final RecipeSerializer<BoreLanceRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate lance;

    public BoreLanceRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
            ItemStackTemplate lance) {
        super(commonInfo, bookInfo, pattern, lance);
        this.lance = lance;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (var stack : input.items()) {
            if (stack.is(ItemTags.SPEARS)) {
                var made = TransmuteRecipe.createWithOriginalComponents(lance, stack);
                EnchantmentHelper.updateEnchantments(made, enchantments -> enchantments.removeIf(enchantment -> enchantment.is(Enchantments.LUNGE)));
                return made;
            }
        }
        return lance.create();
    }

    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        @SuppressWarnings("unchecked")
        var serializer = (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) SERIALIZER;
        return serializer;
    }
}
