package com.saywhat.customguiscales.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.saywhat.customguiscales.Config;
import com.saywhat.customguiscales.CustomGuiScales;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * Rescales individual HUD overlays (hotbar, xp bar, health, ...) by wrapping the vanilla render
 * of each overlay in a scaled pose.
 *
 * <p>These overlays are all anchored to the bottom-centre of the screen, so we scale around that
 * anchor point: the element grows / shrinks in place instead of sliding toward the top-left
 * corner.
 *
 * <p>Pre runs at LOWEST priority so that if another mod cancels the overlay we never push an
 * unbalanced pose; Post runs at HIGHEST so we restore the pose before any other mod draws its
 * own overlay for that element.
 */
@Mod.EventBusSubscriber(modid = CustomGuiScales.MODID, value = Dist.CLIENT)
public final class HudScaleHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        float m = ScaleUtil.multiplier(desiredScaleFor(event.getOverlay().id()));
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
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        float m = ScaleUtil.multiplier(desiredScaleFor(event.getOverlay().id()));
        if (m == 1.0f) {
            return;
        }
        event.getGuiGraphics().pose().popPose();
    }

    /**
     * The whole bottom HUD cluster. These all anchor to the bottom-centre, so scaling them by the
     * same factor around the same point keeps them aligned with each other - i.e. they behave as
     * one "hotbar" unit.
     *
     * <p>In Forge 1.20.1 the experience level number is drawn by the EXPERIENCE_BAR overlay itself
     * (ForgeGui#renderExperience), so it scales together with the bar.
     */
    private static final Set<ResourceLocation> HOTBAR_CLUSTER = Set.of(
            VanillaGuiOverlay.HOTBAR.id(),
            VanillaGuiOverlay.ITEM_NAME.id(),
            VanillaGuiOverlay.JUMP_BAR.id(),
            VanillaGuiOverlay.EXPERIENCE_BAR.id(),
            VanillaGuiOverlay.PLAYER_HEALTH.id(),
            VanillaGuiOverlay.ARMOR_LEVEL.id(),
            VanillaGuiOverlay.FOOD_LEVEL.id(),
            VanillaGuiOverlay.MOUNT_HEALTH.id(),
            VanillaGuiOverlay.AIR_LEVEL.id());

    /** Maps a vanilla overlay id to the configured absolute scale, or 0 if we don't touch it. */
    private static double desiredScaleFor(ResourceLocation overlay) {
        return HOTBAR_CLUSTER.contains(overlay) ? Config.get(Config.HOTBAR_SCALE) : 0.0;
    }

    private HudScaleHandler() {}
}
