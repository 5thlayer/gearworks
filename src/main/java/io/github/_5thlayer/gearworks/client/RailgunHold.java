// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github._5thlayer.gearworks.gear.GearContent;
import io.github._5thlayer.gearworks.railgun.Railgun;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * A Railgun in either hand is held in both, raised to aim, as a loaded crossbow is; and in first person,
 * Charged in the main hand and not swinging, it moves in toward the middle of the view as a loaded crossbow
 * does, by vanilla's numbers for it.
 */
final class RailgunHold implements IClientItemExtensions {

    private RailgunHold() {
    }

    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new RailgunHold(), GearContent.RAILGUN.get());
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick,
            float equipProcess, float swingProcess) {
        if (!Railgun.charged(stack) || arm != player.getMainArm() || swingProcess >= 0.001F) {
            return false;
        }
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(invert * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
        poseStack.translate(invert * -0.641864F, 0.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(invert * 10.0F));
        return true;
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }
}
