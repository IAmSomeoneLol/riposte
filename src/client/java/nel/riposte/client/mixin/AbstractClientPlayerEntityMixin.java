package nel.riposte.client.mixin;

import nel.riposte.ParryData;
import nel.riposte.FinisherData;
import nel.riposte.client.RiposteClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void riposte$lockBodyWithHead(CallbackInfo ci) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;

        boolean isParrying = player instanceof ParryData data && data.getParryTimestamp() > 0;
        boolean isFinisher = player instanceof FinisherData fData && fData.isExecutingFinisher();
        boolean isAnimRunning = RiposteClient.isAnimationActive(player);

        if (isAnimRunning || isParrying || isFinisher) {
            float yaw = player.getYaw();
            float prevYaw = player.prevYaw;

            player.bodyYaw = yaw;
            player.prevBodyYaw = prevYaw;
            player.headYaw = yaw;
            player.prevHeadYaw = prevYaw;
        }
    }
}