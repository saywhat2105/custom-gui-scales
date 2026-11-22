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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rescales the bottom HUD cluster (hotbar, xp bar, health, hunger, ...) by wrapping the render of
 * each of its overlays in a pose scaled around the bottom-centre anchor, so the cluster grows /
 * shrinks in place as one unit.
 *
 * <p>The scaled pose covers the overlay's WHOLE render, including what other mods draw for that
 * overlay from its {@link RenderGuiOverlayEvent.Pre Pre}/{@link RenderGuiOverlayEvent.Post Post}
 * events: Pre pushes at HIGHEST priority and Post pops at LOWEST. AppleSkin, for example, draws its
 * exhaustion underlay in the food overlay's Pre and its hunger/saturation and health overlays in the
 * food and health overlays' Post, so all of it now scales with the icons it decorates. Because a
 * cancelled Pre means Post never fires, a LOWEST Pre handler that also receives cancelled events
 * pops in that case, and any push whose Post still never arrived (e.g. the overlay threw, which
 * ForgeGui logs and skips Post for) is unwound at the next overlay's Pre (a new GuiGraphics is
 * created every frame, so nothing can leak across frames).
 *
 * <p>Besides the vanilla overlays, the cluster includes any overlay ids listed in the
 * {@code extraHotbarLayers} config option, for mods that register their own overlays on the bottom
 * HUD. Such mods position their overlays like vanilla (relative to the bottom-centre), so the same
 * transform keeps them aligned with the scaled vanilla icons.
 */
@Mod.EventBusSubscriber(modid = CustomGuiScales.MODID, value = Dist.CLIENT)
public final class HudScaleHandler {

    /**
     * Vanilla overlays that make up the bottom HUD cluster. These all anchor to the bottom-centre,
     * so scaling them by the same factor around the same point keeps them aligned with each other.
     *
     * <p>In Forge 1.20.1 the experience level number is drawn by the EXPERIENCE_BAR overlay itself
     * (ForgeGui#renderExperience), and the spectator action label is drawn by the ITEM_NAME overlay
     * (it calls SpectatorGui#renderTooltip in spectator mode), so both scale with the cluster.
     */
    private static final Set<ResourceLocation> VANILLA_CLUSTER = Set.of(
            VanillaGuiOverlay.HOTBAR.id(),
            VanillaGuiOverlay.ITEM_NAME.id(),
            VanillaGuiOverlay.JUMP_BAR.id(),
            VanillaGuiOverlay.EXPERIENCE_BAR.id(),
            VanillaGuiOverlay.PLAYER_HEALTH.id(),
            VanillaGuiOverlay.ARMOR_LEVEL.id(),
            VanillaGuiOverlay.FOOD_LEVEL.id(),
            VanillaGuiOverlay.MOUNT_HEALTH.id(),
            VanillaGuiOverlay.AIR_LEVEL.id());

    /** Overlay we pushed a scaled pose for and have not popped yet (null = nothing pushed). */
    private static ResourceLocation pushedOverlay;
    /** The GuiGraphics that push was made on. */
    private static GuiGraphics pushedGraphics;

    /** Parsed {@code extraHotbarLayers}, rebuilt whenever the config list changes. */
    private static List<? extends String> extraSource;
    private static Set<ResourceLocation> extraOverlays = Set.of();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        GuiGraphics graphics = event.getGuiGraphics();
        unwindStalePush(graphics);
        ResourceLocation overlay = event.getOverlay().id();
        if (!isHotbarOverlay(overlay)) {
            return;
        }
        float m = ScaleUtil.multiplier(Config.get(Config.HOTBAR_SCALE));
        if (m == 1.0f) {
            return;
        }
        Window window = Minecraft.getInstance().getWindow();
        float anchorX = window.getGuiScaledWidth() / 2.0f;
        float anchorY = window.getGuiScaledHeight();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(anchorX, anchorY, 0.0f);
        pose.scale(m, m, 1.0f);
        pose.translate(-anchorX, -anchorY, 0.0f);
        pushedOverlay = overlay;
        pushedGraphics = graphics;
    }

    /** Another handler cancelled the overlay, so its Post will never fire: pop now. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onOverlayPreCancelled(RenderGuiOverlayEvent.Pre event) {
        if (event.isCanceled()) {
            popIfPushed(event.getOverlay().id(), event.getGuiGraphics());
        }
    }

    /** LOWEST so every other mod's Post drawing for this overlay happens while still scaled. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOverlayPost(RenderGuiOverlayEvent.Post event) {
        popIfPushed(event.getOverlay().id(), event.getGuiGraphics());
    }

    private static void popIfPushed(ResourceLocation overlay, GuiGraphics graphics) {
        if (pushedOverlay != null && pushedOverlay.equals(overlay) && pushedGraphics == graphics) {
            graphics.pose().popPose();
            pushedOverlay = null;
            pushedGraphics = null;
        }
    }

    /**
     * Defensive: a push whose Post never arrived (e.g. a handler registered after ours at LOWEST
     * cancelled the overlay, or the overlay threw). Unwind it on the same frame's GuiGraphics; a
     * previous frame's GuiGraphics is already gone, so just forget it.
     */
    private static void unwindStalePush(GuiGraphics graphics) {
        if (pushedOverlay != null) {
            if (pushedGraphics == graphics) {
                graphics.pose().popPose();
            }
            pushedOverlay = null;
            pushedGraphics = null;
        }
    }

    private static boolean isHotbarOverlay(ResourceLocation overlay) {
        return VANILLA_CLUSTER.contains(overlay) || extraOverlays().contains(overlay);
    }

    private static Set<ResourceLocation> extraOverlays() {
        List<? extends String> current = Config.getList(Config.EXTRA_HOTBAR_LAYERS);
        if (current != extraSource) {
            Set<ResourceLocation> parsed = new HashSet<>();
            for (String id : current) {
                ResourceLocation rl = ResourceLocation.tryParse(id.trim());
                if (rl != null) {
                    parsed.add(rl);
                }
            }
            extraOverlays = Set.copyOf(parsed);
            extraSource = current;
        }
        return extraOverlays;
    }

    private HudScaleHandler() {}
}
