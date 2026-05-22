package nel.riposte.config;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import net.minecraft.util.Identifier;
import nel.riposte.Riposte;

public class RiposteConfig extends Config {

    public RiposteConfig() {
        super(new Identifier(Riposte.MOD_ID, "main"));
    }

    public ConfigGroup parrySettingsGroup = new ConfigGroup("parrySettings");
    public int parryWindowMs = 175;
    public int parryCooldownMs = 1000;
    public double parryKnockback = 1.0;
    public boolean allowParryWhileUsingItem = false;
    @ConfigGroup.Pop
    public boolean enforceKnockback = true;

    public ConfigGroup parryAdaptionsGroup = new ConfigGroup("parryAdaptions");
    public boolean hitstop = true;
    public int hitstopMs = 30;
    public boolean deflectProjectiles = true;
    public boolean kickCombo = true;
    public double kickComboKnockbackMultiplier = 1.5;
    public int kickComboWindowMs = 1000;
    @ConfigGroup.Pop
    public boolean allowMultiParry = false;

    public ConfigGroup accessoriesConfigGroup = new ConfigGroup("accessoriesConfig");
    public int ironPlateWindowBonusMs = 50;
    @ConfigGroup.Pop
    public float wanderersCapeCooldownCharge = 0.25f;
}