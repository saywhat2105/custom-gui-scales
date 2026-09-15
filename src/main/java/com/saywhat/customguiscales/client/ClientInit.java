package com.saywhat.customguiscales.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Client-only setup. Kept in its own class so the client-only Forge types it references are
 * never classloaded on a dedicated server.
 */
public final class ClientInit {

    public static void init() {
        // Makes the "Config" button on this mod's entry in the in-game Mods list open our own
        // vanilla-styled settings section (the same screen reachable from Video Settings).
        // Forge 1.20.1 has no built-in configuration screen, so we register our own factory.
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> new GuiScalesScreen(parent, mc.options)));

        // HudScaleHandler / SettingsIntegration register themselves via @Mod.EventBusSubscriber;
        // the tooltip mixin is wired up through customguiscales.mixins.json. Nothing else to do.
    }

    private ClientInit() {}
}
