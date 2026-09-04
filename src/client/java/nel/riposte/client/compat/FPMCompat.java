package nel.riposte.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Field;

public class FPMCompat {
    private static Field enabledField = null;
    private static boolean initialized = false;

    public static boolean isFpmEnabled() {
        // 1. Is the mod installed?
        if (!FabricLoader.getInstance().isModLoaded("firstperson")) return false;

        // 2. Fetch the toggle state directly from Tr7zw's core class
        if (!initialized) {
            try {
                Class<?> coreClass = Class.forName("dev.tr7zw.firstperson.FirstPersonModelCore");
                enabledField = coreClass.getField("enabled");
            } catch (Throwable t) {
                // Ignore if we can't find it (version differences)
            }
            initialized = true;
        }

        if (enabledField != null) {
            try {
                return enabledField.getBoolean(null); // Returns true if toggled ON, false if toggled OFF
            } catch (Throwable t) {
                return true;
            }
        }
        return true; // Fallback: Assume enabled if the mod is present
    }
}