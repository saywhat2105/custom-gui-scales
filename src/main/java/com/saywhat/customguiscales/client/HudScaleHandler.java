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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Rescales the bottom HUD cluster (hotbar, xp bar, health, hunger, ...) by wrapping the render of
 * each of its layers in a pose scaled around the bottom-centre anchor, so the cluster grows /
 * shrinks in place as one unit.
 *
 * <p>The scaled pose covers the layer's WHOLE render, including what other mods draw for that layer
 * from its Pre/Post events (e.g. overlays that decorate the hunger bar): Pre pushes at HIGHEST
 * priority and Post pops at LOWEST. Because a cancelled Pre means Post never fires, a LOWEST Pre
 * handler that also receives cancelled events pops in that case, and any push whose Post still
 * never arrived is unwound at the next layer (a new GuiGraphicsExtractor is created every frame, so
 * nothing can leak across frames).
 *
 * <p>Besides the vanilla layers, the cluster includes layers other mods register to draw on top of
 * it - AppleSkin's by default, plus any ids listed in the {@code extraHotbarLayers} config option.
 * Those mods position their layers exactly like vanilla (relative to the bottom-centre), so the same
 * transform keeps them aligned with the scaled vanilla icons.
 *
 * <p>Since the 1.21.6 GUI rewrite the GUI pose is a 2D {@link Matrix3x2fStack} (no z component).
 * In 26.x the class is called {@link GuiGraphicsExtractor} (formerly GuiGraphics): GUI draw calls
 * are extracted into a render state with the pose current at submit time, so scaling the pose
 * around the layer's render still resizes exactly that layer.
 */
@EventBusSubscriber(modid = CustomGuiScales.MODID, value = Dist.CLIENT)
public final class HudScaleHandler {

    /**
     * Vanilla layers that make up the bottom HUD cluster. These all anchor to the bottom-centre, so
     * scaling them by the same factor around the same point keeps them aligned with each other.
     *
     * <p>In 1.21.6+ the experience bar and the jump meter (plus the new locator bar) are drawn by
     * the "contextual info bar" layers, which replaced the old EXPERIENCE_BAR / JUMP_METER layers.
     * Checked against 26.2 (Hud#registerVanillaLayers): the bottom cluster is exactly these eleven
     * layers; the HOTBAR layer also draws the spectator hotbar and SPECTATOR_TOOLTIP its action
     * label. AFTER_CAMERA_DECORATIONS only starts a new GUI stratum and draws nothing, so it is not
     * part of the cluster.
     */
    private static final Set<Identifier> VANILLA_CLUSTER = Set.of(
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

    /**
     * Known mod layers that draw on the cluster. AppleSkin registers these around the vanilla health
     * and food layers: two that only record vanilla's bar heights, and the health-restored,
     * hunger-restored, saturation (the outline around the hunger icons) and exhaustion overlays.
     * Ids of mods that are not installed simply never occur.
     */
    private static final Set<Identifier> KNOWN_MOD_LAYERS = Set.of(
            Identifier.fromNamespaceAndPath("appleskin", "health_offset"),
            Identifier.fromNamespaceAndPath("appleskin", "food_offset"),
            Identifier.fromNamespaceAndPath("appleskin", "health_restored"),
            Identifier.fromNamespaceAndPath("appleskin", "hunger_restored"),
            Identifier.fromNamespaceAndPath("appleskin", "saturation_level"),
            Identifier.fromNamespaceAndPath("appleskin", "exhaustion_level"));

    /** Layer we pushed a scaled pose for and have not popped yet (null = nothing pushed). */
    private static Identifier pushedLayer;
    /** The GuiGraphicsExtractor that push was made on. */
    private static GuiGraphicsExtractor pushedGraphics;

    /** Parsed {@code extraHotbarLayers}, rebuilt whenever the config list changes. */
    private static List<? extends String> extraSource;
    private static Set<Identifier> extraLayers = Set.of();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        unwindStalePush(graphics);
        if (!isHotbarLayer(event.getName())) {
            return;
        }
        float m = ScaleUtil.multiplier(Config.get(Config.HOTBAR_SCALE));
        if (m == 1.0f) {
            return;
        }
        Window window = Minecraft.getInstance().getWindow();
        float anchorX = window.getGuiScaledWidth() / 2.0f;
        float anchorY = window.getGuiScaledHeight();

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(anchorX, anchorY);
        pose.scale(m, m);
        pose.translate(-anchorX, -anchorY);
        pushedLayer = event.getName();
        pushedGraphics = graphics;
    }

    /** Another handler cancelled the layer, so its Post will never fire: pop now. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onLayerPreCancelled(RenderGuiLayerEvent.Pre event) {
        if (event.isCanceled()) {
            popIfPushed(event.getName(), event.getGuiGraphics());
        }
    }

    /** LOWEST so every other mod's Post overlay for this layer is drawn while still scaled. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLayerPost(RenderGuiLayerEvent.Post event) {
        popIfPushed(event.getName(), event.getGuiGraphics());
    }

    private static void popIfPushed(Identifier layer, GuiGraphicsExtractor graphics) {
        if (pushedLayer != null && pushedLayer.equals(layer) && pushedGraphics == graphics) {
            graphics.pose().popMatrix();
            pushedLayer = null;
            pushedGraphics = null;
        }
    }

    /**
     * Defensive: a push whose Post never arrived (e.g. a handler registered after ours at LOWEST
     * cancelled the layer). Unwind it on the same frame's GuiGraphicsExtractor; a previous frame's
     * GuiGraphicsExtractor is already gone, so just forget it.
     */
    private static void unwindStalePush(GuiGraphicsExtractor graphics) {
        if (pushedLayer != null) {
            if (pushedGraphics == graphics) {
                graphics.pose().popMatrix();
            }
            pushedLayer = null;
            pushedGraphics = null;
        }
    }

    private static boolean isHotbarLayer(Identifier layer) {
        return VANILLA_CLUSTER.contains(layer) || KNOWN_MOD_LAYERS.contains(layer) || extraLayers().contains(layer);
    }

    private static Set<Identifier> extraLayers() {
        List<? extends String> current = Config.getList(Config.EXTRA_HOTBAR_LAYERS);
        if (current != extraSource) {
            Set<Identifier> parsed = new HashSet<>();
            for (String id : current) {
                Identifier parsedId = Identifier.tryParse(id.trim());
                if (parsedId != null) {
                    parsed.add(parsedId);
                }
            }
            extraLayers = Set.copyOf(parsed);
            extraSource = current;
        }
        return extraLayers;
    }

    private HudScaleHandler() {}
}
