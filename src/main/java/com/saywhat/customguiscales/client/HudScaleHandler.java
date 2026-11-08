package com.saywhat.customguiscales.client;

import com.mojang.blaze3d.platform.Window;
import com.saywhat.customguiscales.Config;
import com.saywhat.customguiscales.CustomGuiScales;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix3x2fStack;

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
 *
 * <p>Since the 1.21.6 GUI rewrite the GUI pose is a 2D {@link Matrix3x2fStack} (no z component).
 * In 26.x the class is called {@link GuiGraphicsExtractor} (formerly GuiGraphics): GUI draw calls
 * are extracted into a render state with the pose current at submit time, so scaling the pose
 * around the layer's render still resizes exactly that layer.
 */
@EventBusSubscriber(modid = CustomGuiScales.MODID, value = Dist.CLIENT)
public final class HudScaleHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        float m = ScaleUtil.multiplier(desiredScaleFor(event.getName()));
        if (m == 1.0f) {
            return;
        }
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Window window = Minecraft.getInstance().getWindow();
        float anchorX = window.getGuiScaledWidth() / 2.0f;
        float anchorY = window.getGuiScaledHeight();

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(anchorX, anchorY);
        pose.scale(m, m);
        pose.translate(-anchorX, -anchorY);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLayerPost(RenderGuiLayerEvent.Post event) {
        float m = ScaleUtil.multiplier(desiredScaleFor(event.getName()));
        if (m == 1.0f) {
            return;
        }
        event.getGuiGraphics().pose().popMatrix();
    }

    /**
     * The whole bottom HUD cluster. These all anchor to the bottom-centre, so scaling them by the
     * same factor around the same point keeps them aligned with each other - i.e. they behave as
     * one "hotbar" unit.
     *
     * <p>In 1.21.6+ the experience bar and the jump meter (plus the new locator bar) are drawn by
     * the "contextual info bar" layers, which replaced the old EXPERIENCE_BAR / JUMP_METER layers.
     * Checked against 26.2 (Hud#registerVanillaLayers): the bottom cluster is still exactly these ten
     * layers; the HOTBAR layer also draws the spectator hotbar. AFTER_CAMERA_DECORATIONS only starts
     * a new GUI stratum and draws nothing, so it is not part of the cluster.
     */
    private static final Set<Identifier> HOTBAR_CLUSTER = Set.of(
            VanillaGuiLayers.HOTBAR,
            VanillaGuiLayers.SELECTED_ITEM_NAME,
            VanillaGuiLayers.SPECTATOR_TOOLTIP,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR,
            VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.ARMOR_LEVEL,
            VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH,
            VanillaGuiLayers.AIR_LEVEL);

    /** Maps a vanilla layer id to the configured absolute scale, or 0 if we don't touch it. */
    private static double desiredScaleFor(Identifier layer) {
        return HOTBAR_CLUSTER.contains(layer) ? Config.get(Config.HOTBAR_SCALE) : 0.0;
    }

    private HudScaleHandler() {}
}
