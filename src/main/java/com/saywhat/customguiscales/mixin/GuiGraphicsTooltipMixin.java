package com.saywhat.customguiscales.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.saywhat.customguiscales.Config;
import com.saywhat.customguiscales.client.ScaleUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wraps the whole tooltip render (background + text) in a scaled pose so tooltips can use their
 * own GUI scale.
 *
 * <p>We target the private funnel {@code renderTooltipInternal} that every public tooltip method
 * eventually calls. The injectors deliberately capture <i>no</i> target parameters (only the
 * {@link CallbackInfo}) so this keeps working even if NeoForge tweaks that method's argument list
 * across builds. The scale anchor is read straight from the cursor position instead.
 *
 * <p>HEAD always pushes a pose and RETURN always pops it, so the matrix stack stays balanced even
 * for early returns (e.g. an empty tooltip).
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsTooltipMixin {

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"))
    private void customguiscales$scaleTooltipStart(CallbackInfo ci) {
        GuiGraphics self = (GuiGraphics) (Object) this;
        PoseStack pose = self.pose();
        pose.pushPose();

        float m = ScaleUtil.multiplier(Config.get(Config.TOOLTIP_SCALE));
        if (m == 1.0f) {
            return;
        }

        // Scale around the cursor so the tooltip grows / shrinks toward the pointer.
        Minecraft mc = Minecraft.getInstance();
        double screenW = mc.getWindow().getScreenWidth();
        double screenH = mc.getWindow().getScreenHeight();
        float cursorX = screenW <= 0 ? 0
                : (float) (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / screenW);
        float cursorY = screenH <= 0 ? 0
                : (float) (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / screenH);

        pose.translate(cursorX, cursorY, 0.0f);
        pose.scale(m, m, 1.0f);
        pose.translate(-cursorX, -cursorY, 0.0f);
    }

    @Inject(method = "renderTooltipInternal", at = @At("RETURN"))
    private void customguiscales$scaleTooltipEnd(CallbackInfo ci) {
        ((GuiGraphics) (Object) this).pose().popPose();
    }
}
