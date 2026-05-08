package nel.riposte.mixin;

import nel.riposte.ParryData;
import nel.riposte.Riposte;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements ParryData {

    @Unique
    private long parryTimestamp = 0L;

    @Unique
    private long successfulParryTimestamp = 0L;

    @Unique
    private int lastParriedEntityId = -1;

    @Override
    public long getParryTimestamp() {
        return this.parryTimestamp;
    }

    @Override
    public void setParryTimestamp(long timestamp) {
        this.parryTimestamp = timestamp;
    }

    @Override
    public long getSuccessfulParryTimestamp() {
        return this.successfulParryTimestamp;
    }

    @Override
    public void setSuccessfulParryTimestamp(long timestamp) {
        this.successfulParryTimestamp = timestamp;
    }

    @Override
    public int getLastParriedEntityId() {
        return this.lastParriedEntityId;
    }

    @Override
    public void setLastParriedEntityId(int id) {
        this.lastParriedEntityId = id;
    }

    // COMBO TRIGGER
    @Inject(method = "attack", at = @At("TAIL"))
    private void riposte$onAttack(Entity target, CallbackInfo ci) {
        if (Riposte.CONFIG.comboParryEnabled && target instanceof LivingEntity livingTarget) {

            if (target.getId() == this.lastParriedEntityId) {
                long timeSinceParry = System.currentTimeMillis() - this.successfulParryTimestamp;

                if (timeSinceParry <= Riposte.CONFIG.comboParryWindowMs) {

                    PlayerEntity player = (PlayerEntity) (Object) this;
                    double heavyKnockback = Riposte.CONFIG.parryKnockback * Riposte.CONFIG.comboParryMultiplier;

                    // Routeer
                    this.applyParryKnockback(livingTarget, heavyKnockback, player.getX() - livingTarget.getX(), player.getZ() - livingTarget.getZ());

                    this.lastParriedEntityId = -1;

                    // TODO: Trigger Player Animator leg kick animation here
                    // TODO: Trigger VFX and Heavy Sound here
                }
            }
        }
    }
}