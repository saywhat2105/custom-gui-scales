package com.saywhat.customguiscales.client;

import net.minecraft.client.Minecraft;

/**
 * Helper math shared by the HUD layer handler and the tooltip mixin.
 */
public final class ScaleUtil {

    /**
     * Returns the factor to multiply the current pose by so that an element ends up rendered at
     * the given <b>absolute</b> GUI scale.
     *
     * <p>Because the game already applies the global GUI scale to the pose, we only need the
     * ratio {@code desired / globalGuiScale}. Returns {@code 1.0} (no change) when there is
     * nothing to do.
     *
     * @param desiredScale the wanted absolute GUI scale, or {@code <= 0} for "no override"
     */
    public static float multiplier(double desiredScale) {
        if (desiredScale <= 0.0) {
            return 1.0f;
        }
        double globalGuiScale = Minecraft.getInstance().getWindow().getGuiScale();
        if (globalGuiScale <= 0.0) {
            return 1.0f;
        }
        float m = (float) (desiredScale / globalGuiScale);
        // Treat anything essentially equal to 1 as a no-op so we can skip the pose push entirely.
        return Math.abs(m - 1.0f) < 1.0e-4f ? 1.0f : m;
    }

    private ScaleUtil() {}
}
