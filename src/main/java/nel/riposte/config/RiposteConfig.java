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

    public enum FinisherRestrictionMode {
        WHITELIST,
        BLACKLIST,
        DISABLED
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

        public FinisherRestrictionMode finisherAllowExecution = FinisherRestrictionMode.WHITELIST;

        public float finisherHealthThresholdPercent = 35.0f;

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

        /**
         * Central authority for "can this entity be finished off?" - used by both the server
         * (actual execution gate) and the client (prompt visibility / keybind handling).
         * WHITELIST: only entities in finisherWhitelistMobs are allowed.
         * BLACKLIST: entities in finisherWhitelistMobs are NOT allowed, everything else is.
         * DISABLED: no restriction at all, any LivingEntity is eligible.
         */
        public boolean isFinisherAllowedFor(String entityId) {
            return switch (this.finisherAllowExecution) {
                case WHITELIST -> this.finisherWhitelistMobs.contains(entityId);
                case BLACKLIST -> !this.finisherWhitelistMobs.contains(entityId);
                case DISABLED -> true;
            };
        }

        /**
         * Gate on the TARGET's current health percentage, not the player's.
         * Gauge/parry-count filling is untouched by this - it always accumulates from hits landed.
         * This only controls whether a finisher is allowed to actually EXECUTE (and, client-side,
         * whether the prompt is shown at all) once the meter/count requirement is already met.
         * Setting this to 100 effectively disables the requirement, since health can't exceed 100%
         * under normal vanilla conditions.
         */
        public boolean isHealthEligible(float currentHealthPercent) {
            return currentHealthPercent <= this.finisherHealthThresholdPercent;
        }
    }
}