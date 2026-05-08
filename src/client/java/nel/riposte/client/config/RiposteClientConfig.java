package nel.riposte.client.config;

import me.fzzyhmstrs.fzzy_config.config.Config;
import nel.riposte.Riposte;
import net.minecraft.util.Identifier;

public class RiposteClientConfig extends Config {

    public RiposteClientConfig() {
        super(new Identifier(Riposte.MOD_ID, "client"));
    }

    public enum ExecutionMode {
        KEYBIND,
        CAMERA
    }

    public enum IconMode {
        STATIC,
        DYNAMIC
    }

    // Execution Mode
    public ExecutionMode executionMode = ExecutionMode.KEYBIND;

    // HUD Icon Settings
    public IconMode iconMode = IconMode.STATIC;

    // Centered by default (0, 0 offset). y shifted
    public int iconXOffset = 0;
    public int iconYOffset = 20;
    public float iconScale = 1.0f;
}