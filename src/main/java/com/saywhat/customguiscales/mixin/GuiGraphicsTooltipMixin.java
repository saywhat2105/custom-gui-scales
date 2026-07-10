package com.saywhat.customguiscales.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.saywhat.customguiscales.Config;
import com.saywhat.customguiscales.client.ScaleUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Wraps the whole tooltip render (background + text) in a scaled pose so tooltips can use their
 * own GUI scale.
 *
 * <p>We anchor on the {@code mouseX}/{@code mouseY} arguments actually passed into
 * {@code renderTooltipInternal}, NOT the raw screen cursor. Those are in the same coordinate space
 * the tooltip is drawn in, so this stays correct even when another mod (e.g. a guidebook) renders
 * its tooltips inside its own translated/scaled pose.
 *
 * <p>HEAD always pushes a pose and RETURN always pops it, so the matrix stack stays balanced even
 * for early returns (e.g. an empty tooltip).
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsTooltipMixin {

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"))
    private void customguiscales$scaleTooltipStart(Font font, List<ClientTooltipComponent> components,
                                                   int mouseX, int mouseY, ClientTooltipPositioner positioner,
                                                   CallbackInfo ci) {
        GuiGraphics self = (GuiGraphics) (Object) this;
        PoseStack pose = self.pose();
        pose.pushPose();

        float m = ScaleUtil.multiplier(Config.get(Config.TOOLTIP_SCALE));
        if (m == 1.0f) {
            return;
        }
        // Scale around the tooltip's own anchor point (in the current pose space).
        pose.translate(mouseX, mouseY, 0.0f);
        pose.scale(m, m, 1.0f);
        pose.translate(-mouseX, -mouseY, 0.0f);
    }

    @Inject(method = "renderTooltipInternal", at = @At("RETURN"))
    private void customguiscales$scaleTooltipEnd(CallbackInfo ci) {
        ((GuiGraphics) (Object) this).pose().popPose();
    }
}
