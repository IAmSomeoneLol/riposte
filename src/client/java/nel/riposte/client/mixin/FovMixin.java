package nel.riposte.client.mixin;

import nel.riposte.ParryData;
import nel.riposte.client.RiposteClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class FovMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void riposte$applyParryFovPunch(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (RiposteClient.CLIENT_CONFIG.fovChange) {
            ParryData data = (ParryData) client.player;
            long timeSince = System.currentTimeMillis() - data.getSuccessfulParryTimestamp();
            int totalDuration = RiposteClient.CLIENT_CONFIG.fovDurationMs;

            if (timeSince < totalDuration) {
                float intensity;
                float snapTime = 40.0f;

                if (timeSince <= snapTime) {
                    // Match the camera's Sine Ease-Out
                    intensity = (float) Math.sin((timeSince / snapTime) * (Math.PI / 2.0));
                } else {
                    // Match the camera's Cubic Ease-Out
                    float progress = (timeSince - snapTime) / (totalDuration - snapTime);
                    intensity = (float) Math.pow(1.0f - progress, 3);
                }

                double currentFov = cir.getReturnValue();
                cir.setReturnValue(currentFov + (RiposteClient.CLIENT_CONFIG.fovZoom * intensity));
            }
        }
    }
}