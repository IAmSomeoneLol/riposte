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

    public enum FlashType {
        WHITE,
        BLACK
    }

    public ExecutionMode parryActivation = ExecutionMode.KEYBIND;

    public ConfigGroup iconCooldownGroup = new ConfigGroup("iconCooldown");
    public IconMode iconMode = IconMode.STATIC;
    public int xOffset = 0;
    public int yOffset = 20;
    @ConfigGroup.Pop
    public float iconScale = 1.0f;

    public ConfigGroup cameraControlGroup = new ConfigGroup("cameraControl");

    // RESTORED: Camera Shake Settings
    public boolean cameraShake = true;
    public float shakeIntensity = 4.0f;
    public int shakeDurationMs = 200;

    // Camera Recoil Settings
    public boolean cameraRecoil = true;
    public float recoilIntensity = 4.0f;
    public int recoilDurationMs = 300;

    // FOV Settings
    public boolean fovChange = true;
    public double fovZoom = 20.0;
    public int fovDurationMs = 300;

    // Shader Settings
    public boolean lethalParryShader = true;
    @ConfigGroup.Pop
    public int lethalShaderDurationMs = 200;

    public ConfigGroup flashScreenGroup = new ConfigGroup("flashScreen");
    public boolean screenFlash = true;
    public FlashType screenFlashType = FlashType.WHITE;
    @ConfigGroup.Pop
    public int screenFlashDurationMs = 150;

    public ConfigGroup particleConfigGroup = new ConfigGroup("particleConfig");
    public boolean particleNormal = true;
    @ConfigGroup.Pop
    public boolean particleHeavy = true;
}