package nel.riposte;

import dev.emi.trinkets.api.TrinketsApi;
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

    void setLeatherSockTicks(int ticks);
    int getLeatherSockTicks();

    default boolean isParryActive(int windowMs) {
        long timeSinceParry = System.currentTimeMillis() - getParryTimestamp();
        return timeSinceParry >= 0 && timeSinceParry <= windowMs;
    }

    default boolean canParry(int cooldownMs) {
        PlayerEntity player = (PlayerEntity) this;

        if (!Riposte.CONFIG.allowParryWhileUsingItem && player.isUsingItem()) {
            return false;
        }

        return System.currentTimeMillis() - getParryTimestamp() >= cooldownMs;
    }

    default int getCalculatedCooldown(int baseCooldownMs) {
        PlayerEntity player = (PlayerEntity) this;
        var component = TrinketsApi.getTrinketComponent(player).orElse(null);
        if (component == null) return baseCooldownMs;

        double rate = 1.0;
        if (component.isEquipped(Riposte.IRON_GUARD)) rate += Riposte.CONFIG.accessories.ironGuard.rechargeRate;
        if (component.isEquipped(Riposte.VOID_GUARD)) rate += Riposte.CONFIG.accessories.voidGuard.rechargeRate;
        if (component.isEquipped(Riposte.COPPER_GUARD)) rate += Riposte.CONFIG.accessories.copperGuard.rechargeRate;

        if (component.isEquipped(Riposte.NEURAL_LINK)) rate *= Riposte.CONFIG.accessories.neuralLink.rechargeMultiplier;

        return (int) (baseCooldownMs / rate);
    }

    default int getCalculatedWindow(int baseWindowMs) {
        PlayerEntity player = (PlayerEntity) this;
        var component = TrinketsApi.getTrinketComponent(player).orElse(null);
        if (component == null) return baseWindowMs;

        if (component.isEquipped(Riposte.BLOODLUSTFUL_RING)) {
            return (int) (baseWindowMs * Riposte.CONFIG.accessories.bloodlustfulRing.windowMultiplier);
        }
        return baseWindowMs;
    }

    default boolean canParryDamageType(DamageSource source) {
        PlayerEntity player = (PlayerEntity) this;
        var component = TrinketsApi.getTrinketComponent(player).orElse(null);
        if (component == null) return false;

        if (component.isEquipped(Riposte.VOID_GUARD)) return true;

        if (source.isOf(DamageTypes.FALL) && (component.isEquipped(Riposte.LEATHER_SOCK) || component.isEquipped(Riposte.COPPER_GUARD))) return true;

        boolean hasPlate = component.isEquipped(Riposte.IRON_GUARD) || component.isEquipped(Riposte.COPPER_GUARD);
        if (source.isIn(DamageTypeTags.IS_PROJECTILE) && hasPlate) return true;

        return source.getAttacker() instanceof LivingEntity && !source.isIn(DamageTypeTags.IS_PROJECTILE) && !source.isIndirect();
    }

    default boolean canParryProjectiles() {
        PlayerEntity player = (PlayerEntity) this;
        var component = TrinketsApi.getTrinketComponent(player).orElse(null);
        if (component == null) return false;

        if (component.isEquipped(Riposte.VOID_GUARD)) return true;
        return component.isEquipped(Riposte.IRON_GUARD) || component.isEquipped(Riposte.COPPER_GUARD);
    }

    default void refundParryCooldown(float percentage) {
        int currentCooldown = getCalculatedCooldown(Riposte.CONFIG.parryCooldownMs);
        long refundAmount = (long) (currentCooldown * percentage);
        setParryTimestamp(getParryTimestamp() - refundAmount);
    }

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