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
    private void riposte$cameraShake(CallbackInfo ci) {
        if (!RiposteClient.CLIENT_CONFIG.cameraShake) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ParryData data = (ParryData) client.player;
        long timeSince = System.currentTimeMillis() - data.getSuccessfulParryTimestamp();

        if (timeSince < RiposteClient.CLIENT_CONFIG.shakeDurationMs) {
            float intensity = 1.0f - ((float) timeSince / RiposteClient.CLIENT_CONFIG.shakeDurationMs);

            float shakeX = (random.nextFloat() - 0.5f) * RiposteClient.CLIENT_CONFIG.shakeIntensity * intensity;
            float shakeY = (random.nextFloat() - 0.5f) * RiposteClient.CLIENT_CONFIG.shakeIntensity * intensity;

            this.setRotation(this.yaw + shakeX, this.pitch + shakeY);
        }
    }
}