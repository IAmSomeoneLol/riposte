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

    // --- NEW: 9-Point HUD Anchor System ---
    public enum AnchorPoint {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    public ExecutionMode parryActivation = ExecutionMode.KEYBIND;

    public ConfigGroup iconCooldownGroup = new ConfigGroup("iconCooldown");
    public AnchorPoint iconAnchor = AnchorPoint.CENTER; // The new anchor setting!
    public IconMode iconMode = IconMode.STATIC;
    public int xOffset = 0;
    public int yOffset = 20;
    public float iconScale = 1.0f;
    @ConfigGroup.Pop
    public boolean playCooldownSound = true;

    public ConfigGroup cameraControlGroup = new ConfigGroup("cameraControl");
    public boolean cameraShake = true;
    public float shakeIntensity = 4.0f;
    public float cameraWalkAmplitude = 0.5f;
    @ConfigGroup.Pop
    public float cameraPushback = 0.8f;

    public ConfigGroup cameraRecoilGroup = new ConfigGroup("cameraRecoil");
    public boolean cameraRecoil = true;
    public float recoilIntensity = 4.0f;
    @ConfigGroup.Pop
    public int recoilDurationMs = 300;

    public ConfigGroup fovSettingsGroup = new ConfigGroup("fovSettings");
    public boolean fovChange = true;
    public double fovZoom = 20.0;
    @ConfigGroup.Pop
    public int fovDurationMs = 300;

    public boolean lethalParryShader = true;
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