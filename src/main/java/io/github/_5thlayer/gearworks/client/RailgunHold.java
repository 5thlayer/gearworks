// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github._5thlayer.gearworks.gear.GearContent;
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
 * in the main hand, Charged or not and swinging or not, it moves in toward the middle of the view as a loaded
 * crossbow does, though not all the way, turned toward the crosshair and its muzzle tilted up so its side shows.
 */
final class RailgunHold implements IClientItemExtensions {

    /**
     * How far of a loaded crossbow's way to the middle a Railgun moves. Seen from straight behind,
     * its upright sprite would show only its edge, its body hiding its barrel; a little to the side, turned
     * toward the crosshair, it shows its side and its rails.
     */
    private static final float CENTRING = 0.6F;

    /** Degrees it turns toward the crosshair; a loaded crossbow turns 10. */
    private static final float YAW = 25.0F;

    /** Degrees it tilts its muzzle up, so the barrel shows over the body. */
    private static final float TILT = 10.0F;

    private RailgunHold() {
    }

    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new RailgunHold(), GearContent.RAILGUN.get());
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick,
            float equipProcess, float swingProcess) {
        if (arm != player.getMainArm()) {
            return false;
        }
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(invert * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
        poseStack.translate(invert * CENTRING * -0.641864F, 0.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(invert * YAW));
        poseStack.mulPose(Axis.XP.rotationDegrees(TILT));
        return true;
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }
}
