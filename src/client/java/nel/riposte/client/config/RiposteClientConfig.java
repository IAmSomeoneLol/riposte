package nel.riposte.client.config;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
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

    public ExecutionMode parryActivation = ExecutionMode.KEYBIND;

    public ConfigGroup iconCooldownGroup = new ConfigGroup("iconCooldown");
    public IconMode iconMode = IconMode.STATIC;
    public int xOffset = 0;
    public int yOffset = 20;
    @ConfigGroup.Pop
    public float iconScale = 1.0f;

    public ConfigGroup vfxSettingsGroup = new ConfigGroup("vfxSettings");
    public boolean cameraShake = true;
    @ConfigGroup.Pop
    public boolean screenFlash = true;
}