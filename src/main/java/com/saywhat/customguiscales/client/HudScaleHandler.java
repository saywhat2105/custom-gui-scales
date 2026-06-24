package com.saywhat.customguiscales.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.saywhat.customguiscales.Config;
import com.saywhat.customguiscales.CustomGuiScales;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Set;

/**
 * Rescales individual HUD layers (hotbar, xp bar, health, ...) by wrapping the vanilla render of
 * each layer in a scaled pose.
 *
 * <p>These layers are all anchored to the bottom-centre of the screen, so we scale around that
 * anchor point: the element grows / shrinks in place instead of sliding toward the top-left
 * corner.
 *
 * <p>Pre runs at LOWEST priority so that if another mod cancels the layer we never push an
 * unbalanced pose; Post runs at HIGHEST so we restore the pose before any other mod draws its
 * own overlay for that layer.
 */
@EventBusSubscriber(modid = CustomGuiScales.MODID, value = Dist.CLIENT)
public final class HudScaleHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        float m = ScaleUtil.multiplier(desiredScaleFor(event.getName()));
        if (m == 1.0f) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Window window = Minecraft.getInstance().getWindow();
        float anchorX = window.getGuiScaledWidth() / 2.0f;
        float anchorY = window.getGuiScaledHeight();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(anchorX, anchorY, 0.0f);
        pose.scale(m, m, 1.0f);
        pose.translate(-anchorX, -anchorY, 0.0f);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLayerPost(RenderGuiLayerEvent.Post event) {
        float m = ScaleUtil.multiplier(desiredScaleFor(event.getName()));
        if (m == 1.0f) {
            return;
        }
        event.getGuiGraphics().pose().popPose();
    }

    /**
     * The whole bottom HUD cluster. These all anchor to the bottom-centre, so scaling them by the
     * same factor around the same point keeps them aligned with each other - i.e. they behave as
     * one "hotbar" unit.
     */
    private static final Set<ResourceLocation> HOTBAR_CLUSTER = Set.of(
            VanillaGuiLayers.HOTBAR,
            VanillaGuiLayers.SELECTED_ITEM_NAME,
            VanillaGuiLayers.JUMP_METER,
            VanillaGuiLayers.EXPERIENCE_BAR,
            VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.ARMOR_LEVEL,
            VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH,
            VanillaGuiLayers.AIR_LEVEL);

    /** Maps a vanilla layer id to the configured absolute scale, or 0 if we don't touch it. */
    private static double desiredScaleFor(ResourceLocation layer) {
        return HOTBAR_CLUSTER.contains(layer) ? Config.get(Config.HOTBAR_SCALE) : 0.0;
    }

    private HudScaleHandler() {}
}
