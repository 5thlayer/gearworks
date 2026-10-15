// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github._5thlayer.gearworks.lance.LungeBoring;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A jab's enchantments, watched for the impulse Lunge gives, so a Bore Lance's Lunge Boring starts when
 * vanilla's rules let its Lunge fire and never otherwise, and run without Lunge's food gate for a lance
 * on FE. No event marks a jab or a lunge.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @WrapOperation(method = "postPiercingAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;doPostPiercingAttackEffects(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void gearworks$lungeBoring(ServerLevel level, LivingEntity user, Operation<Void> original) {
        var before = user.getDeltaMovement();
        LungeBoring.withoutFoodGate(user, () -> original.call(level, user));
        LungeBoring.jabbed(user, before);
    }
}
