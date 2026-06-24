package com.saywhat.customguiscales;

import com.saywhat.customguiscales.client.ClientInit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Custom GUI Scales - lets you render individual HUD elements (hotbar, tooltips, etc.)
 * at their own GUI scale, independent of the global "GUI Scale" video setting.
 *
 * <p>This is a purely client-side mod. On a dedicated server it registers the (unused)
 * client config and does nothing else.
 */
@Mod(CustomGuiScales.MODID)
public final class CustomGuiScales {

    public static final String MODID = "customguiscales";

    public CustomGuiScales(IEventBus modBus, ModContainer container) {
        // Register the client config spec. CLIENT configs are only actually loaded on the
        // physical client; registering on a dedicated server is harmless.
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        // Everything that touches rendering / screens is client-only. Guarding the call keeps
        // the client-only classes from ever being loaded on a dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInit.init(container);
        }
    }
}
