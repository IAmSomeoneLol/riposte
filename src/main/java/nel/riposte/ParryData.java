package nel.riposte;

import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.Vec3d;

public interface ParryData {
    long getParryTimestamp();
    void setParryTimestamp(long timestamp);

    long getSuccessfulParryTimestamp();
    void setSuccessfulParryTimestamp(long timestamp);

    long getSuccessfulComboTimestamp();
    void setSuccessfulComboTimestamp(long timestamp);

    int getLastParriedEntityId();
    void setLastParriedEntityId(int id);

    default boolean isParryActive(int windowMs) {
        long timeSinceParry = System.currentTimeMillis() - getParryTimestamp();
        return timeSinceParry >= 0 && timeSinceParry <= windowMs;
    }

    default boolean canParry(int cooldownMs) {
        return System.currentTimeMillis() - getParryTimestamp() >= cooldownMs;
    }

    default int getCalculatedCooldown(int baseCooldownMs) {
        PlayerEntity player = (PlayerEntity) this;
        var capability = AccessoriesCapability.get(player);
        if (capability == null) return baseCooldownMs;

        double rate = 1.0;

        if (capability.isEquipped(Riposte.IRON_PLATE)) rate += 3.0;
        if (capability.isEquipped(Riposte.CREST_OF_THE_VOID)) rate += 3.0;
        if (capability.isEquipped(Riposte.COBALT_PLATE)) rate += 5.0;

        if (capability.isEquipped(Riposte.BRAIN_CHIP)) rate *= 2.0;

        return (int) (baseCooldownMs / rate);
    }

    default int getCalculatedWindow(int baseWindowMs) {
        PlayerEntity player = (PlayerEntity) this;
        var capability = AccessoriesCapability.get(player);
        if (capability == null) return baseWindowMs;

        if (capability.isEquipped(Riposte.EVERLASTING_BLOODRING)) {
            return baseWindowMs * 2;
        }
        return baseWindowMs;
    }

    default boolean canParryDamageType(DamageSource source) {
        PlayerEntity player = (PlayerEntity) this;
        var capability = AccessoriesCapability.get(player);
        if (capability == null) return false;

        if (capability.isEquipped(Riposte.CREST_OF_THE_VOID)) return true;

        if (source.isOf(DamageTypes.FALL) && capability.isEquipped(Riposte.LEATHER_SOCK)) return true;

        boolean hasPlate = capability.isEquipped(Riposte.IRON_PLATE) || capability.isEquipped(Riposte.COBALT_PLATE);
        if (source.isIn(DamageTypeTags.IS_PROJECTILE) && hasPlate) return true;

        return source.getAttacker() instanceof LivingEntity && !source.isIn(DamageTypeTags.IS_PROJECTILE) && !source.isIndirect();
    }

    default boolean canParryProjectiles() {
        PlayerEntity player = (PlayerEntity) this;
        var capability = AccessoriesCapability.get(player);
        if (capability == null) return false;

        if (capability.isEquipped(Riposte.CREST_OF_THE_VOID)) return true;
        return capability.isEquipped(Riposte.IRON_PLATE) || capability.isEquipped(Riposte.COBALT_PLATE);
    }

    default void refundParryCooldown(float percentage) {
        // Flattened reference here
        int currentCooldown = getCalculatedCooldown(Riposte.CONFIG.parryCooldownMs);
        long refundAmount = (long) (currentCooldown * percentage);
        setParryTimestamp(getParryTimestamp() - refundAmount);
    }

    default void applyParryKnockback(LivingEntity target, double strength, double x, double z) {
        // Flattened reference here
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