package com.saywhat.customguiscales.client;

import com.saywhat.customguiscales.Config;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.SimpleOptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * A vanilla-styled settings sub-screen with an absolute-scale slider for each customizable
 * element. The global "GUI Scale" stays in its normal Video Settings spot; this screen holds the
 * per-element overrides.
 *
 * <p>Each slider is an integer 0-8 where 0 means "Off (use the global scale)". Values are written
 * to the config live while dragging (so the change previews instantly) and persisted to disk when
 * the screen closes.
 */
public class GuiScalesScreen extends SimpleOptionsSubScreen {

    public GuiScalesScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Component.translatable("customguiscales.screen.title"),
                new OptionInstance<?>[] {
                        slider("customguiscales.config.hotbarScale", Config.HOTBAR_SCALE),
                        slider("customguiscales.config.tooltipScale", Config.TOOLTIP_SCALE)
                });
    }

    private static OptionInstance<Integer> slider(String captionKey, ForgeConfigSpec.DoubleValue cfg) {
        int initial = clamp((int) Math.round(Config.get(cfg)));
        return new OptionInstance<>(
                captionKey,
                OptionInstance.noTooltip(),
                // Sliders only display what this returns, so include the caption ("Name: value").
                (caption, value) -> Options.genericValueLabel(caption, value == 0
                        ? Component.translatable("customguiscales.value.off")
                        : Component.literal(value + "×")),
                new OptionInstance.IntRange(0, 8),
                initial,
                value -> {
                    if (Config.SPEC.isLoaded()) {
                        cfg.set((double) value.intValue());
                    }
                });
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(8, v));
    }

    @Override
    public void removed() {
        super.removed();
        // Persist our config to disk (the live edits above only changed the in-memory values).
        if (Config.SPEC.isLoaded()) {
            Config.SPEC.save();
        }
    }
}
