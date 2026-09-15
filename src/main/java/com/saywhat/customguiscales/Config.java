package com.saywhat.customguiscales;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client config. Every value is an <b>absolute</b> GUI scale, exactly like the vanilla
 * "GUI Scale" video option (1, 2, 3, 4, ...). A value of {@code 0} means "leave this element
 * at the normal/global GUI scale" (no override).
 */
public final class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    /** Scale of the whole bottom HUD cluster (hotbar, xp bar, health, hunger, armor, air, ...). */
    public static final ForgeConfigSpec.DoubleValue HOTBAR_SCALE;
    /** Scale of item / text tooltips. */
    public static final ForgeConfigSpec.DoubleValue TOOLTIP_SCALE;

    public static final ForgeConfigSpec SPEC;

    private static final double MIN = 0.0;
    private static final double MAX = 8.0;

    static {
        BUILDER.comment(
                "Custom GUI Scales",
                "",
                "Each value is an ABSOLUTE GUI scale, just like the vanilla 'GUI Scale' video option.",
                "Example: a value of 3 makes that element render as if GUI Scale were 3, no matter what",
                "your global GUI Scale is set to.",
                "Set a value to 0 to leave that element at your normal/global GUI scale (no override).",
                "Allowed range: " + MIN + " - " + MAX);
        BUILDER.push("scales");

        HOTBAR_SCALE = BUILDER
                .comment("Scale of the hotbar and the rest of the bottom HUD (xp bar, health, hunger,",
                         "armor, air, mount health/jump bar) - they all scale together as one unit.")
                .translation("customguiscales.config.hotbarScale")
                .defineInRange("hotbarScale", 3.0, MIN, MAX);

        TOOLTIP_SCALE = BUILDER
                .comment("Scale of item / text tooltips (for example the ones in your inventory).")
                .translation("customguiscales.config.tooltipScale")
                .defineInRange("tooltipScale", 2.0, MIN, MAX);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /**
     * Reads a config value safely. Returns {@code 0.0} (= "no override") if the config has not
     * been loaded yet, so callers never have to deal with {@link IllegalStateException}.
     */
    public static double get(ForgeConfigSpec.DoubleValue value) {
        try {
            return value.get();
        } catch (IllegalStateException notLoadedYet) {
            return 0.0;
        }
    }

    private Config() {}
}
