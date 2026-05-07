package nel.riposte.config;

import me.fzzyhmstrs.fzzy_config.config.Config;
import net.minecraft.util.Identifier;
import nel.riposte.Riposte;

public class RiposteConfig extends Config {

    public RiposteConfig() {
        super(new Identifier(Riposte.MOD_ID, "main"));
    }

    // Mechanics Settings
    public int parryWindowMs = 175;
    public int parryCooldownMs = 1000;
    public double parryKnockback = 0.5;
    public boolean enforceKnockback = true;

    // Accessory Buffs
    public int ironPlateWindowBonusMs = 50;
    public int leatherSockCooldownReductionMs = 250;

    // Hitstop Settings
    public boolean hitstopEnabled = true;
    public int hitstopMs = 175;

    // Combo Parry Settings
    public boolean comboParryEnabled = true;
    public double comboParryMultiplier = 1.5;
    public int comboParryWindowMs = 1000;
}