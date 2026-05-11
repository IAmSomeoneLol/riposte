package nel.riposte.client.mixin;

import nel.riposte.ParryData;
import nel.riposte.client.RiposteClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow private float pitch;
    @Shadow private float yaw;

    private final Random random = new Random();

    @Inject(method = "update", at = @At("TAIL"))
    private void riposte$cameraEffects(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ParryData data = (ParryData) client.player;
        long timeSince = System.currentTimeMillis() - data.getSuccessfulParryTimestamp();

        float totalX = 0f;
        float totalY = 0f;

        // 1. Calculate the Smooth Directional Recoil
        if (RiposteClient.CLIENT_CONFIG.cameraRecoil) {
            int recoilDuration = RiposteClient.CLIENT_CONFIG.recoilDurationMs;

            if (timeSince < recoilDuration) {
                float intensity;
                float snapTime = 40.0f;

                if (timeSince <= snapTime) {
                    intensity = (float) Math.sin((timeSince / snapTime) * (Math.PI / 2.0));
                } else {
                    float progress = (timeSince - snapTime) / (recoilDuration - snapTime);
                    intensity = (float) Math.pow(1.0f - progress, 3);
                }

                totalX += RiposteClient.recoilDirX * RiposteClient.CLIENT_CONFIG.recoilIntensity * intensity;
                totalY += RiposteClient.recoilDirY * RiposteClient.CLIENT_CONFIG.recoilIntensity * intensity;
            }
        }

        // 2. Calculate the Violent Random Shake
        if (RiposteClient.CLIENT_CONFIG.cameraShake) {
            int shakeDuration = RiposteClient.CLIENT_CONFIG.shakeDurationMs;

            if (timeSince < shakeDuration) {
                float intensity = 1.0f - ((float) timeSince / shakeDuration);

                totalX += (random.nextFloat() - 0.5f) * RiposteClient.CLIENT_CONFIG.shakeIntensity * intensity;
                totalY += (random.nextFloat() - 0.5f) * RiposteClient.CLIENT_CONFIG.shakeIntensity * intensity;
            }
        }

        // 3. Apply the combined effects to the camera!
        if (totalX != 0f || totalY != 0f) {
            this.setRotation(this.yaw + totalX, this.pitch + totalY);
        }
    }
}