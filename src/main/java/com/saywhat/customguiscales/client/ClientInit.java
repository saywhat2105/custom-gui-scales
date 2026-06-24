package com.saywhat.customguiscales.client;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only setup. Kept in its own class so the client-only NeoForge types it references are
 * never classloaded on a dedicated server.
 */
public final class ClientInit {

    public static void init(ModContainer container) {
        // Makes the "Config" button on this mod's entry in the in-game Mods list open our own
        // vanilla-styled settings section (the same screen reachable from Video Settings).
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new GuiScalesScreen(parent, Minecraft.getInstance().options));

        // HudScaleHandler / SettingsIntegration register themselves via @EventBusSubscriber; the
        // tooltip mixin is wired up through customguiscales.mixins.json. Nothing else to do here.
    }

    private ClientInit() {}
}
