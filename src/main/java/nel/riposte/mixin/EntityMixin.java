package nel.riposte.mixin;

import nel.riposte.ParryData;
import nel.riposte.Riposte;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void riposte$enlargeHitboxForKickCombo(CallbackInfoReturnable<Box> cir) {
        Entity thisEntity = (Entity) (Object) this;

        if (thisEntity instanceof LivingEntity && thisEntity.getWorld() != null) {
            Box originalBox = cir.getReturnValue();
            if (originalBox != null) {
                List<? extends PlayerEntity> players = thisEntity.getWorld().getPlayers();

                for (PlayerEntity player : players) {
                    ParryData parryData = (ParryData) player;

                    if (parryData.getLastParriedEntityId() == thisEntity.getId()) {
                        long timeSinceParry = System.currentTimeMillis() - parryData.getSuccessfulParryTimestamp();

                        if (timeSinceParry <= Riposte.CONFIG.kickComboWindowMs) {
                            cir.setReturnValue(originalBox.expand(0.3D));
                            break;
                        }
                    }
                }
            }
        }
    }
}