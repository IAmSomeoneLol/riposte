package nel.riposte.config;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import net.minecraft.util.Identifier;
import nel.riposte.Riposte;

import java.util.List;

public class RiposteConfig extends Config {

    public RiposteConfig() {
        super(Identifier.of(Riposte.MOD_ID, "main"));
    }

    public enum FinisherMode { GAUGE_METER, PARRY_COUNT }
    public enum FinisherTrigger { KICK_COMBO, NORMAL_PARRY, BOTH }
    public enum FinisherRestrictionMode { WHITELIST, BLACKLIST, DISABLED }

    public ConfigGroup parrySettingsGroup = new ConfigGroup("parrySettings");
    public int parryWindowMs = 175;
    public int parryCooldownMs = 10000;
    public double parryKnockback = 0.5;
    public boolean allowParryWhileUsingItem = false;
    public boolean enableSuccessParryRecharge = true;
    public double globalParryCooldownRecharge = 0.15; // 30%
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

    public Accessories accessories = new Accessories();
    public Addons addons = new Addons();

    public static class Accessories extends ConfigSection {
        public IronGuard ironGuard = new IronGuard();
        public LeatherSocks leatherSocks = new LeatherSocks();
        public VoidGuard voidGuard = new VoidGuard();
        public CopperGuard copperGuard = new CopperGuard();
        public BloodlustfulRing bloodlustfulRing = new BloodlustfulRing();
        public HonorableCape honorableCape = new HonorableCape();
        public NeuralLink neuralLink = new NeuralLink();
        public ShulkerHeadPlate shulkerHeadPlate = new ShulkerHeadPlate();

        public static class IronGuard extends ConfigSection {
            public double rechargeRate = 3.0;
            public double knockbackMultiplier = 1.3;
            public int invulnTimeTicks = 20;
        }

        public static class LeatherSocks extends ConfigSection {
            public double speedMultiplier = 0.1;
            public int speedDurationTicks = 70;
        }

        public static class VoidGuard extends ConfigSection {
            public double rechargeRate = 5.0;
            public int invulnTimeTicks = 30;
        }

        public static class CopperGuard extends ConfigSection {
            public double rechargeRate = 5.0;
            public int invulnTimeTicks = 30;
        }

        public static class BloodlustfulRing extends ConfigSection {
            public double windowMultiplier = 2.0;
            public double kickDamageMultiplier = 1.6;
            public double damageTakenMultiplier = 1.5;
        }

        public static class HonorableCape extends ConfigSection {
            public double cooldownCharge = 0.25;
            public double damageDealtMultiplier = 1.1;
        }

        public static class NeuralLink extends ConfigSection {
            public double rechargeMultiplier = 2.0;
            public double autoParryChance = 0.5;
        }

        public static class ShulkerHeadPlate extends ConfigSection {
            public int slotBonus = 1;
            public double attackSpeedBoost = 0.2;
            public int attackSpeedDurationTicks = 20;
        }
    }

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
        public float finisherGaugeConsumptionPerSecond = 10.0f;
        public int finisherParryCountMax = 8;
        public int finisherTimeoutMs = 6000;
        public boolean finisherRewardEnabled = true;
        public float finisherHealthReturnPercent = 10.0f;
        public float finisherFoodReturnPercent = 10.0f;
        public FinisherRestrictionMode finisherAllowExecution = FinisherRestrictionMode.WHITELIST;
        public float finisherHealthThresholdPercent = 30.0f;

        public List<String> finisherWhitelistMobs = List.of(
                "minecraft:zombie", "minecraft:zombie_villager", "minecraft:husk",
                "minecraft:skeleton", "minecraft:stray", "minecraft:drowned",
                "minecraft:pillager", "minecraft:vindicator", "minecraft:evoker",
                "minecraft:piglin", "minecraft:piglin_brute"
        );

        public boolean isFinisherAllowedFor(String entityId) {
            return switch (this.finisherAllowExecution) {
                case WHITELIST -> this.finisherWhitelistMobs.contains(entityId);
                case BLACKLIST -> !this.finisherWhitelistMobs.contains(entityId);
                case DISABLED -> true;
            };
        }

        public boolean isHealthEligible(float currentHealthPercent) {
            return currentHealthPercent <= this.finisherHealthThresholdPercent;
        }
    }
}