package com.saywhat.customguiscales.mixin;

import com.saywhat.customguiscales.Config;
import com.saywhat.customguiscales.client.ScaleUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Renders tooltips at their own GUI scale without moving them.
 *
 * <p>Vanilla computes the tooltip's size, asks the positioner where to put it, then draws it with
 * its top-left at that spot. We replicate that exact size + position math from the very same
 * inputs, then scale the pose around that top-left corner. Because the corner is a fixed point of
 * the transform, the tooltip lands exactly where vanilla would have drawn it - only resized. This
 * makes no assumption about coordinate spaces or where the tooltip sits relative to the mouse, so
 * it stays correct even when another mod renders tooltips inside its own transformed pose.
 *
 * <p>Since 1.21.6 tooltips are deferred to the end of the frame ({@code setTooltipForNextFrame}
 * -> {@code renderDeferredTooltip}), but every path still funnels into the NeoForge-patched
 * 7-argument {@code renderTooltip(Font, List, int, int, ClientTooltipPositioner, ResourceLocation,
 * ItemStack)} - the 6-argument overload simply delegates to it - and that is the method that does
 * the size math, calls the positioner and draws the background + text. We target exactly that
 * overload by descriptor so the 6-argument delegate is never double-scaled.
 *
 * <p>(NeoForge fires RenderTooltipEvent.Pre inside this method; if another mod moves the tooltip
 * via that event the anchor could differ slightly, but the default case matches exactly.)
 *
 * <p>HEAD always pushes and RETURN always pops (including the early return when the Pre event is
 * cancelled), so the matrix stack stays balanced.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsTooltipMixin {

    private static final String RENDER_TOOLTIP =
            "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II"
                    + "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;"
                    + "Lnet/minecraft/resources/ResourceLocation;"
                    + "Lnet/minecraft/world/item/ItemStack;)V";

    @Inject(method = RENDER_TOOLTIP, at = @At("HEAD"))
    private void customguiscales$scaleTooltipStart(Font font, List<ClientTooltipComponent> components,
                                                   int mouseX, int mouseY, ClientTooltipPositioner positioner,
                                                   ResourceLocation backgroundTexture, ItemStack tooltipStack,
                                                   CallbackInfo ci) {
        GuiGraphics self = (GuiGraphics) (Object) this;
        Matrix3x2fStack pose = self.pose();
        pose.pushMatrix();

        float m = ScaleUtil.multiplier(Config.get(Config.TOOLTIP_SCALE));
        if (m == 1.0f || components.isEmpty()) {
            return;
        }

        // Same size math as vanilla renderTooltip (1.21.8: getHeight now takes the font).
        int width = 0;
        int height = components.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent component : components) {
            width = Math.max(width, component.getWidth(font));
            height += component.getHeight(font);
        }
        // Same position call as vanilla -> the exact top-left corner it is about to draw at.
        Vector2ic corner = positioner.positionTooltip(self.guiWidth(), self.guiHeight(), mouseX, mouseY, width, height);
        float anchorX = corner.x();
        float anchorY = corner.y();

        pose.translate(anchorX, anchorY);
        pose.scale(m, m);
        pose.translate(-anchorX, -anchorY);
    }

    @Inject(method = RENDER_TOOLTIP, at = @At("RETURN"))
    private void customguiscales$scaleTooltipEnd(CallbackInfo ci) {
        ((GuiGraphics) (Object) this).pose().popMatrix();
    }
}
