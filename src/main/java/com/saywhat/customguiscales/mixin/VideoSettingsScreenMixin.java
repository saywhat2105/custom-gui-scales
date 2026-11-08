package com.saywhat.customguiscales.mixin;

import com.saywhat.customguiscales.client.SettingsIntegration;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the "Custom GUI Scales…" row at the top of the Video Settings options list.
 *
 * <p>{@code OptionsSubScreen#addContents()} creates {@code this.list} and then calls
 * {@code addOptions()}; injecting at the HEAD of {@code VideoSettingsScreen#addOptions()} adds our
 * row before any vanilla row, so it becomes the first entry while every vanilla option (including
 * GUI Scale) keeps its normal place. This runs on every (re-)init, e.g. after a window resize.
 *
 * <p>The mixin extends {@link OptionsSubScreen} (VideoSettingsScreen's superclass) only so that
 * the inherited protected {@code list} field is accessible; the constructor is never called.
 */
@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenMixin extends OptionsSubScreen {

    protected VideoSettingsScreenMixin(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    @Inject(method = "addOptions()V", at = @At("HEAD"))
    private void customguiscales$addOpenButtonRow(CallbackInfo ci) {
        if (this.list != null) {
            SettingsIntegration.addTo(this.list, this);
        }
    }
}
