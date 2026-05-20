package nel.riposte.client.mixin;

import nel.riposte.ParryData;
import nel.riposte.client.RiposteClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow protected abstract void moveBy(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow private float pitch;
    @Shadow private float yaw;

    @Inject(method = "update", at = @At("TAIL"))
    private void riposte$applyEssenceOfParryCamera(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ParryData data = (ParryData) client.player;
        long timeSince = System.currentTimeMillis() - data.getSuccessfulParryTimestamp();

        float finalPitchOffset = 0f;
        float finalYawOffset = 0f;
        float finalXOffset = 0f;
        float finalYOffset = 0f;
        float finalZOffset = 0f;

        if (RiposteClient.CLIENT_CONFIG.cameraRecoil && timeSince < RiposteClient.CLIENT_CONFIG.recoilDurationMs) {
            float progress = (float) timeSince / RiposteClient.CLIENT_CONFIG.recoilDurationMs;
            float ease = (float) Math.pow(1.0 - progress, 3);
            finalPitchOffset -= RiposteClient.CLIENT_CONFIG.recoilIntensity * ease;
        }

        if (RiposteClient.CLIENT_CONFIG.cameraShake && timeSince < 250) {
            float progress = (float) timeSince / 250f;
            float fade = Math.max(0.0f, 1.0f - progress);

            float noiseX = (float) (Math.sin(timeSince * 0.1) * Math.cos(timeSince * 0.05));
            float noiseY = (float) (Math.cos(timeSince * 0.13) * Math.sin(timeSince * 0.07));

            finalPitchOffset += noiseX * RiposteClient.CLIENT_CONFIG.shakeIntensity * fade * 3.0f;
            finalYawOffset += noiseY * RiposteClient.CLIENT_CONFIG.shakeIntensity * fade * 3.0f;

            if (!thirdPerson) {
                finalXOffset = noiseX * RiposteClient.CLIENT_CONFIG.cameraWalkAmplitude * fade;
                finalYOffset = noiseY * RiposteClient.CLIENT_CONFIG.cameraWalkAmplitude * fade;
                finalZOffset = -RiposteClient.CLIENT_CONFIG.cameraPushback * fade;
            }
        }

        if (!thirdPerson) {
            this.moveBy(finalXOffset, finalYOffset, finalZOffset);
        }

        this.setRotation(this.yaw + finalYawOffset, this.pitch + finalPitchOffset);
    }
}