// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github._5thlayer.gearworks.bench.GearBenchBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The suit a Gear Bench holds ({@link GearBenchBlockEntity#shown}), floating above it at half an item's
 * size, turning and bobbing, lit as the block above it is.
 */
final class GearBenchRenderer implements BlockEntityRenderer<GearBenchBlockEntity, GearBenchRenderer.RenderState> {

    /** Where the suit floats, in block units above the bench's base. */
    private static final double FLOAT_HEIGHT = 1.3;
    /** How far it bobs above and below {@link #FLOAT_HEIGHT}, in block units. */
    private static final double BOB = 0.06;
    /** Ticks per bob, up and back down. */
    private static final float BOB_TICKS = 40;
    /** Degrees it turns each tick. */
    private static final float TURN = 3;
    /** Ticks after which both the bob and the turn are back where they began, so the time drawn stays small. */
    private static final long CYCLE_TICKS = 120;
    /** Its size, against an item's own. */
    private static final float SCALE = 0.5F;

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(GearBenchBlockEntity bench, RenderState state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(bench, state, partialTicks, cameraPosition, breakProgress);
        state.item = null;
        var level = bench.getLevel();
        var suit = bench.shown();
        if (level == null || suit.isEmpty()) {
            return;
        }
        var item = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(item, suit, ItemDisplayContext.FIXED, level, null, 0);
        state.item = item;
        // A small time that loops, as a float holding the whole game time would lose the partial tick.
        state.time = level.getGameTime() % CYCLE_TICKS + partialTicks;
        state.light = LevelRenderer.getLightCoords(level, bench.getBlockPos().above());
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        if (state.item == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5, FLOAT_HEIGHT + BOB * Mth.sin(state.time * Mth.TWO_PI / BOB_TICKS), 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.time * TURN % 360));
        poseStack.scale(SCALE, SCALE, SCALE);
        state.item.submit(poseStack, collector, state.light, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /** The suit floats over the bench, so it is drawn while the space above is in sight. */
    @Override
    public AABB getRenderBoundingBox(GearBenchBlockEntity bench) {
        return new AABB(bench.getBlockPos()).expandTowards(0, 1, 0);
    }

    static final class RenderState extends BlockEntityRenderState {
        private @Nullable ItemStackRenderState item;
        private float time;
        private int light;
    }
}
