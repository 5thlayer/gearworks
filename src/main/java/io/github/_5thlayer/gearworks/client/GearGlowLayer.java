// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github._5thlayer.gearworks.Gearworks;
import io.github._5thlayer.gearworks.gear.Gear;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The worn suit's gem, lit whatever the light around it and pulsing at {@link GlowPulse}'s pace: a
 * fullbright pass over the chest armor model, posed as vanilla's armor layer poses the suit, that
 * blends {@code entity/gear_glow.png}, a brighter gem and a faint halo, over it.
 */
final class GearGlowLayer<S extends HumanoidRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {

    /**
     * Vanilla's eyes, fullbright and blended, but nudged toward the camera as armor is: without the
     * armor's view offset the glow sits behind the suit's pixels and shows only where it has none.
     */
    private static final RenderType GLOW = RenderType.create(Gearworks.MOD_ID + "_gem_glow",
            RenderSetup.builder(RenderPipelines.EYES)
                    .withTexture("Sampler0", Identifier.fromNamespaceAndPath(Gearworks.MOD_ID, "textures/entity/gear_glow.png"))
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .sortOnUpload()
                    .createRenderSetup());

    private final Model<? super S> chest;
    private final Model<? super S> babyChest;

    private GearGlowLayer(RenderLayerParent<S, M> parent, Model<? super S> chest, Model<? super S> babyChest) {
        super(parent);
        this.chest = chest;
        this.babyChest = babyChest;
    }

    /** Lights the gem on players, mannequins and armor stands, every entity that shows a worn suit. */
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        var models = event.getEntityModels();
        for (var skin : event.getSkins()) {
            addTo(event.<AvatarRenderer<AbstractClientPlayer>>getPlayerRenderer(skin), playerChest(skin, models));
            addTo(event.<AvatarRenderer<ClientMannequin>>getMannequinRenderer(skin), playerChest(skin, models));
        }
        ArmorStandRenderer stand = event.getRenderer(EntityType.ARMOR_STAND);
        if (stand != null) {
            stand.addLayer(new GearGlowLayer<ArmorStandRenderState, ArmorStandArmorModel>(stand,
                    ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR, models, ArmorStandArmorModel::new).chest(),
                    ArmorModelSet.bake(ModelLayers.ARMOR_STAND_SMALL_ARMOR, models, ArmorStandArmorModel::new).chest()));
        }
    }

    private static PlayerModel playerChest(PlayerModelType skin, EntityModelSet models) {
        boolean slim = skin == PlayerModelType.SLIM;
        var layers = slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR;
        return ArmorModelSet.bake(layers, models, part -> new PlayerModel(part, slim)).chest();
    }

    private static void addTo(AvatarRenderer<?> renderer, PlayerModel chest) {
        if (renderer != null) {
            renderer.addLayer(new GearGlowLayer<AvatarRenderState, PlayerModel>(renderer, chest, chest));
        }
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
        if (!Gear.is(state.chestEquipment)) {
            return;
        }
        collector.order(1).submitModel(state.isBaby ? babyChest : chest, state, poseStack, GLOW, LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, GlowPulse.color(state.ageInTicks), null, state.outlineColor, null);
    }
}
