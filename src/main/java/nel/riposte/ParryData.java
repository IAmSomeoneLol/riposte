package nel.riposte;

import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.Vec3d;

public interface ParryData {
    // Basic Parry Timing
    long getParryTimestamp();
    void setParryTimestamp(long timestamp);

    // Combo Parry Memory
    long getSuccessfulParryTimestamp();
    void setSuccessfulParryTimestamp(long timestamp);
    int getLastParriedEntityId();
    void setLastParriedEntityId(int id);

    // Calculates if the current time falls within the active parry frames
    default boolean isParryActive(int windowMs) {
        long timeSinceParry = System.currentTimeMillis() - getParryTimestamp();
        return timeSinceParry >= 0 && timeSinceParry <= windowMs;
    }

    // Calculates if enough time has passed to parry again
    default boolean canParry(int cooldownMs) {
        return System.currentTimeMillis() - getParryTimestamp() >= cooldownMs;
    }

    // --- ACCESSORY STAT CALCULATORS ---

    // 1. Calculates the actual cooldown based on Recharge Rate bonuses
    default int getCalculatedCooldown(int baseCooldownMs) {
        PlayerEntity player = (PlayerEntity) this;
        var capability = AccessoriesCapability.get(player);
        if (capability == null) return baseCooldownMs;

        double rate = 1.0; // Base recharge rate

        if (capability.isEquipped(Riposte.IRON_PLATE)) rate += 3.0;
        if (capability.isEquipped(Riposte.CREST_OF_THE_VOID)) rate += 3.0;
        if (capability.isEquipped(Riposte.COBALT_PLATE)) rate += 5.0;

        if (capability.isEquipped(Riposte.BRAIN_CHIP)) rate *= 2.0; // +100%

        return (int) (baseCooldownMs / rate);
    }

    // 2. Calculates the actual forgiveness window
    default int getCalculatedWindow(int baseWindowMs) {
        PlayerEntity player = (PlayerEntity) this;
        var capability = AccessoriesCapability.get(player);
        if (capability == null) return baseWindowMs;

        if (capability.isEquipped(Riposte.EVERLASTING_BLOODRING)) {
            return baseWindowMs * 2; // +2x Window Length
        }
        return baseWindowMs;
    }

    // 3. Filters what damage is legally allowed to be parried
    default boolean canParryDamageType(DamageSource source) {
        PlayerEntity player = (PlayerEntity) this;
        var capability = AccessoriesCapability.get(player);
        if (capability == null) return false;

        // Crest of the Void overrides everything: Parries ALL damage
        if (capability.isEquipped(Riposte.CREST_OF_THE_VOID)) return true;

        // Leather Socks: Allows fall damage
        if (source.isOf(DamageTypes.FALL) && capability.isEquipped(Riposte.LEATHER_SOCK)) return true;

        // Iron/Cobalt Plate: Allows projectiles (arrows, fireballs, etc.)
        boolean hasPlate = capability.isEquipped(Riposte.IRON_PLATE) || capability.isEquipped(Riposte.COBALT_PLATE);
        if (source.isIn(DamageTypeTags.IS_PROJECTILE) && hasPlate) return true;

        // DEFAULT ENGINE RULES: Only parry direct entity melee attacks.
        // This stops you from parrying fire, drowning, void, and standard fall damage.
        return source.getAttacker() instanceof LivingEntity && !source.isIn(DamageTypeTags.IS_PROJECTILE) && !source.isIndirect();
    }

    // --- KNOCKBACK ENGINE ---
    default void applyParryKnockback(LivingEntity target, double strength, double x, double z) {
        if (Riposte.CONFIG.enforceKnockback) {
            target.velocityModified = true;

            Vec3d currentVel = target.getVelocity();
            Vec3d kbVec = (new Vec3d(x, 0.0, z)).normalize().multiply(strength);

            target.setVelocity(
                    currentVel.x / 2.0 - kbVec.x,
                    target.isOnGround() ? Math.min(0.4, currentVel.y / 2.0 + strength) : currentVel.y,
                    currentVel.z / 2.0 - kbVec.z
            );
        } else {
            target.takeKnockback(strength, x, z);
        }
    }
}