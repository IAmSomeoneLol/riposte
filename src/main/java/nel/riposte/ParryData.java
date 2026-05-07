package nel.riposte;

import net.minecraft.entity.LivingEntity;
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

    // Custom Knockback Application
    default void applyParryKnockback(LivingEntity target, double strength, double x, double z) {
        if (Riposte.CONFIG.enforceKnockback) {
            // Tells the server to forcibly update the client's position
            target.velocityModified = true;

            // Replicating vanilla knockback math without the resistance reduction
            Vec3d currentVel = target.getVelocity();
            Vec3d kbVec = (new Vec3d(x, 0.0, z)).normalize().multiply(strength);

            target.setVelocity(
                    currentVel.x / 2.0 - kbVec.x,
                    target.isOnGround() ? Math.min(0.4, currentVel.y / 2.0 + strength) : currentVel.y,
                    currentVel.z / 2.0 - kbVec.z
            );
        } else {
            // Respects Netherite/Warden resistance
            target.takeKnockback(strength, x, z);
        }
    }
}