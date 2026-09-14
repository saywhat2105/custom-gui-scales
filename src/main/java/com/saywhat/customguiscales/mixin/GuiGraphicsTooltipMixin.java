package com.saywhat.customguiscales.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.saywhat.customguiscales.Config;
import com.saywhat.customguiscales.client.ScaleUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.ResourceLocation;
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
 * <p>(NeoForge fires RenderTooltipEvent.Pre inside this method; if another mod moves the tooltip
 * via that event the anchor could differ slightly, but the default case matches exactly.)
 *
 * <p>HEAD always pushes and RETURN always pops, so the matrix stack stays balanced.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsTooltipMixin {

    // 1.21.4: renderTooltipInternal(Font, List<ClientTooltipComponent>, int, int, ClientTooltipPositioner,
    // @Nullable ResourceLocation backgroundTexture). The parameter list here must match it exactly.
    @Inject(method = "renderTooltipInternal", at = @At("HEAD"))
    private void customguiscales$scaleTooltipStart(Font font, List<ClientTooltipComponent> components,
                                                   int mouseX, int mouseY, ClientTooltipPositioner positioner,
                                                   ResourceLocation backgroundTexture, CallbackInfo ci) {
        GuiGraphics self = (GuiGraphics) (Object) this;
        PoseStack pose = self.pose();
        pose.pushPose();

        float m = ScaleUtil.multiplier(Config.get(Config.TOOLTIP_SCALE));
        if (m == 1.0f || components.isEmpty()) {
            return;
        }

        // Same size math as vanilla renderTooltipInternal.
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

        pose.translate(anchorX, anchorY, 0.0f);
        pose.scale(m, m, 1.0f);
        pose.translate(-anchorX, -anchorY, 0.0f);
    }

    @Inject(method = "renderTooltipInternal", at = @At("RETURN"))
    private void customguiscales$scaleTooltipEnd(CallbackInfo ci) {
        ((GuiGraphics) (Object) this).pose().popPose();
    }
}
