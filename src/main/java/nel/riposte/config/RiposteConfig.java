package nel.riposte.config;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import net.minecraft.util.Identifier;
import nel.riposte.Riposte;

import java.util.List;

public class RiposteConfig extends Config {

    public RiposteConfig() {
        super(new Identifier(Riposte.MOD_ID, "main"));
    }

    public enum FinisherMode {
        GAUGE_METER,
        PARRY_COUNT
    }

    public enum FinisherTrigger {
        KICK_COMBO,
        NORMAL_PARRY,
        BOTH
    }

    public ConfigGroup parrySettingsGroup = new ConfigGroup("parrySettings");
    public int parryWindowMs = 175;
    public int parryCooldownMs = 10000;
    public double parryKnockback = 0.5;
    public boolean allowParryWhileUsingItem = false;
    @ConfigGroup.Pop
    public boolean enforceKnockback = true;

    public ConfigGroup parryAdaptionsGroup = new ConfigGroup("parryAdaptions");
    public boolean hitstop = true;
    public int hitstopMs = 45;
    public boolean deflectProjectiles = true;
    public boolean kickCombo = true;
    public double kickComboKnockbackMultiplier = 1.5;
    public int kickComboWindowMs = 550;
    @ConfigGroup.Pop
    public boolean allowMultiParry = false;

    public ConfigGroup accessoriesConfigGroup = new ConfigGroup("accessoriesConfig");
    public int ironPlateWindowBonusMs = 50;
    @ConfigGroup.Pop
    public float wanderersCapeCooldownCharge = 0.25f;

    public Addons addons = new Addons();

    public static class Addons extends ConfigSection {
        public Finishers finishers = new Finishers();
    }

    public static class Finishers extends ConfigSection {
        public boolean enableFinishers = true;
        public boolean enemyDamageFinisher = false;

        public FinisherMode finisherMode = FinisherMode.GAUGE_METER;
        public FinisherTrigger finisherFillOn = FinisherTrigger.BOTH;

        public float finisherFillOnParry = 25.0f;
        public float finisherFillOnKickCombo = 25.0f;
        public float finisherGaugeConsumptionPerSecond = 15.0f;

        public int finisherParryCountMax = 5;
        public int finisherTimeoutMs = 6000;

        // --- NEW: Finisher Reward Configs ---
        public boolean finisherRewardEnabled = true;
        public float finisherHealthReturnPercent = 10.0f;
        public float finisherFoodReturnPercent = 10.0f;

        public List<String> finisherWhitelistMobs = List.of(
                "minecraft:zombie",
                "minecraft:zombie_villager",
                "minecraft:husk",
                "minecraft:skeleton",
                "minecraft:stray",
                "minecraft:drowned",
                "minecraft:pillager",
                "minecraft:vindicator",
                "minecraft:evoker",
                "minecraft:piglin",
                "minecraft:piglin_brute"
        );
    }
}