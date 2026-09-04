package nel.riposte.client.mixin;

import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import nel.riposte.Riposte;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void riposte$lockBodyYaw(CallbackInfo ci) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;

        var animationContainer = PlayerAnimationAccess.getPlayerAssociatedData(player).get(Identifier.of(Riposte.MOD_ID, "animation"));

        if (animationContainer != null && animationContainer.isActive()) {
            player.bodyYaw = player.headYaw;
            player.prevBodyYaw = player.prevHeadYaw;
        }
    }
}