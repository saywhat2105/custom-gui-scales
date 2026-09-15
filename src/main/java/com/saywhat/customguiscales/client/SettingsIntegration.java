package com.saywhat.customguiscales.client;

import com.mojang.serialization.Codec;
import com.saywhat.customguiscales.CustomGuiScales;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Adds a full-width "Custom GUI Scales…" row to the top of the Video Settings options list, which
 * opens {@link GuiScalesScreen}. The vanilla GUI Scale option is left exactly where it normally
 * is.
 *
 * <p>1.20.1's {@link OptionsList} only accepts {@link OptionInstance}s (there is no
 * {@code addSmall(AbstractWidget, AbstractWidget)} like in later versions), so the row is an
 * {@link OptionInstance} whose {@link OptionInstance.ValueSet} builds a plain {@link Button}
 * instead of a slider / cycle button.
 */
@Mod.EventBusSubscriber(modid = CustomGuiScales.MODID, value = Dist.CLIENT)
public final class SettingsIntegration {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof VideoSettingsScreen video)) {
            return;
        }

        for (GuiEventListener child : video.children()) {
            if (child instanceof OptionsList list) {
                // addBig appends a full-width row at the bottom; rotate the (live) list by one
                // so our new last row moves to the top while every other row keeps its relative
                // order. This avoids referencing OptionsList.Entry, which is a protected type.
                list.addBig(openButtonOption(video));
                Collections.rotate(list.children(), 1);
                return;
            }
        }
    }

    /** An option whose "widget" is just a button opening {@link GuiScalesScreen}. */
    private static OptionInstance<Boolean> openButtonOption(VideoSettingsScreen video) {
        return new OptionInstance<>(
                "customguiscales.button.open",
                OptionInstance.noTooltip(),
                (caption, value) -> caption,
                new OpenScreenValueSet(video),
                Boolean.FALSE,
                value -> {});
    }

    /**
     * A {@link OptionInstance.ValueSet} that creates a plain button. The value itself is never
     * changed or displayed; it only exists to satisfy the {@link OptionInstance} contract.
     */
    private record OpenScreenValueSet(VideoSettingsScreen video) implements OptionInstance.ValueSet<Boolean> {

        @Override
        public Function<OptionInstance<Boolean>, AbstractWidget> createButton(
                OptionInstance.TooltipSupplier<Boolean> tooltipSupplier, Options options,
                int x, int y, int width, Consumer<Boolean> onValueChanged) {
            return option -> {
                Minecraft mc = Minecraft.getInstance();
                return Button.builder(
                                Component.translatable("customguiscales.button.open"),
                                b -> mc.setScreen(new GuiScalesScreen(video, mc.options)))
                        .bounds(x, y, width, 20)
                        .tooltip(Tooltip.create(Component.translatable("customguiscales.button.tooltip")))
                        .build();
            };
        }

        @Override
        public Optional<Boolean> validateValue(Boolean value) {
            return Optional.of(Boolean.FALSE);
        }

        @Override
        public Codec<Boolean> codec() {
            return Codec.BOOL;
        }
    }

    private SettingsIntegration() {}
}
