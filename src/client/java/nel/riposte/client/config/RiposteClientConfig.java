package nel.riposte.client.config;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import nel.riposte.Riposte;
import net.minecraft.util.Identifier;

public class RiposteClientConfig extends Config {

    public RiposteClientConfig() {
        super(new Identifier(Riposte.MOD_ID, "client"));
    }

    public enum ExecutionMode { KEYBIND, CAMERA }
    public enum IconMode { STATIC, DYNAMIC }
    public enum FlashType { WHITE, BLACK }
    public enum AnchorPoint {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    public ExecutionMode parryActivation = ExecutionMode.KEYBIND;

    public boolean firstPersonAnimations = true;

    public ConfigGroup iconCooldownGroup = new ConfigGroup("iconCooldown");
    public AnchorPoint iconAnchor = AnchorPoint.BOTTOM_CENTER;
    public IconMode iconMode = IconMode.STATIC;
    public int xOffset = 100;
    public int yOffset = -12;
    public float iconScale = 1.1f;
    @ConfigGroup.Pop
    public boolean playCooldownSound = true;

    public ConfigGroup cameraControlGroup = new ConfigGroup("cameraControl");
    public boolean cameraShake = true;
    public float shakeIntensity = 1.001f;
    public float cameraWalkAmplitude = 0.1f;
    @ConfigGroup.Pop
    public float cameraPushback = 0.0f;

    public ConfigGroup cameraRecoilGroup = new ConfigGroup("cameraRecoil");
    public boolean cameraRecoil = true;
    public float recoilIntensity = 70.5f;
    public int recoilDurationMs = 750;
    @ConfigGroup.Pop
    public boolean parryFallCameraFlow = true;

    public ConfigGroup fovSettingsGroup = new ConfigGroup("fovSettings");
    public boolean fovChange = true;
    public double fovZoom = 30.0;
    @ConfigGroup.Pop
    public int fovDurationMs = 1900;

    public boolean lethalParryShader = true;
    public int lethalShaderDurationMs = 100;

    public ConfigGroup flashScreenGroup = new ConfigGroup("flashScreen");
    public boolean screenFlash = true;
    public FlashType screenFlashType = FlashType.WHITE;
    @ConfigGroup.Pop
    public int screenFlashDurationMs = 250;

    public Particles particles = new Particles();
    public Addons addons = new Addons();

    public static class Particles extends ConfigSection {
        public boolean parryFallParticle = true;

        public boolean particleNormal = true;
        public int normalParticleCount = 25;
        public double normalParticleVelocity = 0.65;

        public boolean particleHeavy = true;
        public int heavyParticleCount = 30;
        public double heavyParticleVelocity = 3.5;
    }

    public static class Addons extends ConfigSection {
        public Finishers finishers = new Finishers();
    }

    public static class Finishers extends ConfigSection {
        public float contextualButtonPromptSize = 0.005f;
        public float contextualButtonPromptTextScale = 2.5f;
        public boolean cameraLock = true;
        public long cameraCenterPanningDurationMs = 300;
    }
}