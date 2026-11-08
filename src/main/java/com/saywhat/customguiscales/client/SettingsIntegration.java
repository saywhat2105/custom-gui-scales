package com.saywhat.customguiscales.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Adds a full-width "Custom GUI Scales…" row to the top of the Video Settings options list, which
 * opens {@link GuiScalesScreen}. The vanilla GUI Scale option is left exactly where it normally
 * is.
 *
 * <p>Minecraft 26.x made {@code AbstractSelectionList#children()} an unmodifiable view and stores
 * each entry's position when it is added, so the row can no longer be appended after
 * {@code init()} and rotated to the top. Instead
 * {@link com.saywhat.customguiscales.mixin.VideoSettingsScreenMixin} calls {@link #addTo} at the
 * very start of {@code VideoSettingsScreen#addOptions()}, which makes it the first row and lets
 * vanilla lay out every row that follows (including the section headers) as usual.
 */
public final class SettingsIntegration {

    // 310 is the vanilla full-width option size; a single widget this wide renders as a centered,
    // full-width row (see OptionsList.Entry#extractContent).
    private static final int ROW_WIDTH = 310;
    private static final int ROW_HEIGHT = 20;

    /** Appends the "Custom GUI Scales…" row to {@code list}; {@code parent} is the screen to return to. */
    public static void addTo(OptionsList list, Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        Button button = Button.builder(
                        Component.translatable("customguiscales.button.open"),
                        b -> mc.gui.setScreen(new GuiScalesScreen(parent, mc.options)))
                .bounds(0, 0, ROW_WIDTH, ROW_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("customguiscales.button.tooltip")))
                .build();
        list.addSmall(button, null);
    }

    private SettingsIntegration() {}
}
