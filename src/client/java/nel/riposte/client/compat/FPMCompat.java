package nel.riposte.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import java.lang.reflect.Field;

public class FPMCompat {
    private static Field enabledField = null;
    private static boolean initialized = false;

    public static boolean isFpmEnabled() {
        // Check
        if (!FabricLoader.getInstance().isModLoaded("firstperson")) return false;

        // Fetch
        if (!initialized) {
            try {
                Class<?> coreClass = Class.forName("dev.tr7zw.firstperson.FirstPersonModelCore");
                enabledField = coreClass.getField("enabled");
            } catch (Throwable t) {
                // Ignore
            }
            initialized = true;
        }

        if (enabledField != null) {
            try {
                return enabledField.getBoolean(null);
            } catch (Throwable t) {
                return true;
            }
        }
        // Fallback
        return true;
    }
}