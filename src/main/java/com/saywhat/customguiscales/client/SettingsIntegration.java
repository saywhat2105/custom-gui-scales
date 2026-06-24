package com.saywhat.customguiscales.client;

import com.saywhat.customguiscales.CustomGuiScales;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Collections;

/**
 * Adds a full-width "Custom GUI Scales…" row to the top of the Video Settings options list, which
 * opens {@link GuiScalesScreen}. The vanilla GUI Scale option is left exactly where it normally
 * is.
 */
@EventBusSubscriber(modid = CustomGuiScales.MODID, value = Dist.CLIENT)
public final class SettingsIntegration {

    // 310 is the vanilla full-width option size; a single widget this wide renders as a centered,
    // full-width row (see OptionsList.Entry#render).
    private static final int ROW_WIDTH = 310;
    private static final int ROW_HEIGHT = 20;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof VideoSettingsScreen video)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();

        for (GuiEventListener child : video.children()) {
            if (child instanceof OptionsList list) {
                Button button = Button.builder(
                                Component.translatable("customguiscales.button.open"),
                                b -> mc.setScreen(new GuiScalesScreen(video, mc.options)))
                        .bounds(0, 0, ROW_WIDTH, ROW_HEIGHT)
                        .tooltip(Tooltip.create(Component.translatable("customguiscales.button.tooltip")))
                        .build();
                // addSmall appends at the bottom; rotate the (live) list by one so our new last
                // row moves to the top while every other row keeps its relative order. This avoids
                // referencing OptionsList.Entry, which is a protected type.
                list.addSmall(button, null);
                Collections.rotate(list.children(), 1);
                return;
            }
        }
    }

    private SettingsIntegration() {}
}
