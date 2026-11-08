package com.saywhat.customguiscales.mixin;

import com.saywhat.customguiscales.Config;
import com.saywhat.customguiscales.client.ScaleUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Renders tooltips at their own GUI scale while placing them exactly where vanilla would place a
 * tooltip that was natively that size.
 *
 * <p>Vanilla decides where a tooltip goes (right of the cursor, or flipped to the left / pushed up
 * when it would overflow) based on the tooltip's size. If we only scaled the tooltip down around
 * one corner, that decision would still be the one made for the full-size tooltip - e.g. a long
 * tooltip gets flipped to the left of the cursor and then shrinks away from it, leaving a gap.
 *
 * <p>So: we replicate vanilla's size math, ask the positioner where the FULL-size tooltip will be
 * drawn ({@code full}) and where a tooltip of the SCALED size should go ({@code scaled}), then
 * apply a transform that maps the drawn tooltip's top-left from {@code full} onto {@code scaled}
 * while scaling it. The result flips and clamps correctly for its real rendered size, and always
 * sits adjacent to the cursor on whichever side fits.
 *
 * <p>In 26.x GuiGraphics is called {@link GuiGraphicsExtractor} and {@code renderTooltip} is called
 * {@code tooltip}. Tooltips are still deferred to the end of the frame
 * ({@code setTooltipForNextFrame} -> {@code setTooltipForNextFrameInternal} -> the deferred
 * {@code extractDeferredElements}), and every path still funnels into the NeoForge-patched
 * 7-argument {@code tooltip(Font, List, int, int, ClientTooltipPositioner, Identifier, ItemStack)}
 * - the 6-argument overload simply delegates to it - and that is the method that does the size
 * math, calls the positioner and draws the background + text. We target exactly that overload by
 * descriptor so the 6-argument delegate is never double-scaled.
 *
 * <p>(NeoForge fires RenderTooltipEvent.Pre inside this method; if another mod moves the tooltip
 * via that event the anchor could differ slightly, but the default case matches exactly.)
 *
 * <p>HEAD always pushes and RETURN always pops (including the early return when the Pre event is
 * cancelled), so the matrix stack stays balanced.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsTooltipMixin {

    private static final String TOOLTIP =
            "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II"
                    + "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;"
                    + "Lnet/minecraft/resources/Identifier;"
                    + "Lnet/minecraft/world/item/ItemStack;)V";

    @Inject(method = TOOLTIP, at = @At("HEAD"))
    private void customguiscales$scaleTooltipStart(Font font, List<ClientTooltipComponent> components,
                                                   int mouseX, int mouseY, ClientTooltipPositioner positioner,
                                                   Identifier backgroundTexture, ItemStack tooltipStack,
                                                   CallbackInfo ci) {
        GuiGraphicsExtractor self = (GuiGraphicsExtractor) (Object) this;
        Matrix3x2fStack pose = self.pose();
        pose.pushMatrix();

        float m = ScaleUtil.multiplier(Config.get(Config.TOOLTIP_SCALE));
        if (m == 1.0f || components.isEmpty()) {
            return;
        }

        // Same size math as vanilla GuiGraphicsExtractor#tooltip (26.2: getHeight takes the font).
        int width = 0;
        int height = components.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent component : components) {
            width = Math.max(width, component.getWidth(font));
            height += component.getHeight(font);
        }
        int guiW = self.guiWidth();
        int guiH = self.guiHeight();

        // Where vanilla is about to draw the full-size tooltip (its top-left).
        Vector2ic full = positioner.positionTooltip(guiW, guiH, mouseX, mouseY, width, height);
        // Where a tooltip of the actual rendered size belongs (flip/clamp decided for THAT size).
        int scaledW = Math.round(width * m);
        int scaledH = Math.round(height * m);
        Vector2ic scaled = positioner.positionTooltip(guiW, guiH, mouseX, mouseY, scaledW, scaledH);

        // Map the drawn tooltip (top-left at `full`) onto the scaled placement (top-left at `scaled`).
        pose.translate(scaled.x(), scaled.y());
        pose.scale(m, m);
        pose.translate(-full.x(), -full.y());
    }

    @Inject(method = TOOLTIP, at = @At("RETURN"))
    private void customguiscales$scaleTooltipEnd(CallbackInfo ci) {
        ((GuiGraphicsExtractor) (Object) this).pose().popMatrix();
    }
}
