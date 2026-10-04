package nel.riposte.client.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import nel.riposte.FinisherData;
import nel.riposte.client.RiposteClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void riposte$lockBodyToHead(
            AbstractClientPlayerEntity player,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (RiposteClient.isAnimationActive(player) || (player instanceof FinisherData f && f.isExecutingFinisher())) {
            float cameraYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevYaw, player.getYaw());
            player.bodyYaw = cameraYaw;
            player.prevBodyYaw = cameraYaw;
            player.headYaw = cameraYaw;
            player.prevHeadYaw = cameraYaw;
        }
    }
}