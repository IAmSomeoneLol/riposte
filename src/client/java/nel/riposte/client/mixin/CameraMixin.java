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

import java.util.Random;

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

        float forwardMove = 0f;
        float upMove = 0f;
        float rightMove = 0f;

        if (RiposteClient.CLIENT_CONFIG.cameraRecoil && timeSince < RiposteClient.CLIENT_CONFIG.recoilDurationMs) {
            float progress = (float) timeSince / RiposteClient.CLIENT_CONFIG.recoilDurationMs;
            float ease = (float) Math.pow(1.0 - progress, 3);

            if (RiposteClient.lastParryWasFall) {
                // Slam the crosshair down towards the floor on landing
                finalPitchOffset += RiposteClient.CLIENT_CONFIG.recoilIntensity * ease;
            } else {
                // Random recoil direction for normal combat parries
                Random rand = new Random(data.getSuccessfulParryTimestamp());
                double angle = rand.nextDouble() * Math.PI * 2.0;

                float yawDir = (float) Math.cos(angle);
                float pitchDir = (float) Math.sin(angle);

                finalYawOffset += yawDir * RiposteClient.CLIENT_CONFIG.recoilIntensity * ease;
                finalPitchOffset += pitchDir * RiposteClient.CLIENT_CONFIG.recoilIntensity * ease;
            }
        }

        // Standard combat camera micro-shake (ignored during a fall parry)
        if (RiposteClient.CLIENT_CONFIG.cameraShake && timeSince < 250 && !RiposteClient.lastParryWasFall) {
            float progress = (float) timeSince / 250f;
            float fade = Math.max(0.0f, 1.0f - progress);

            float noiseX = (float) (Math.sin(timeSince * 0.1) * Math.cos(timeSince * 0.05));
            float noiseY = (float) (Math.cos(timeSince * 0.13) * Math.sin(timeSince * 0.07));

            finalPitchOffset += noiseX * RiposteClient.CLIENT_CONFIG.shakeIntensity * fade * 3.0f;
            finalYawOffset += noiseY * RiposteClient.CLIENT_CONFIG.shakeIntensity * fade * 3.0f;

            rightMove = noiseX * RiposteClient.CLIENT_CONFIG.cameraWalkAmplitude * fade;
            upMove = noiseY * RiposteClient.CLIENT_CONFIG.cameraWalkAmplitude * fade;

            forwardMove = -RiposteClient.CLIENT_CONFIG.cameraPushback * fade;
        }

        // NEW: Camera Flow (Downward dip on fall parry to simulate crouching/impact)
        if (RiposteClient.CLIENT_CONFIG.parryFallCameraFlow && RiposteClient.lastParryWasFall && timeSince < 1000) {
            float progress = (float) timeSince / 1000f; // 1 second duration
            float ease = (float) Math.pow(1.0 - progress, 3); // Cubic ease out

            // Instantly shoves the camera down -0.75 blocks, then glides smoothly back to 0
            upMove -= 0.75f * ease;
        }

        this.moveBy(forwardMove, upMove, rightMove);
        this.setRotation(this.yaw + finalYawOffset, this.pitch + finalPitchOffset);
    }
}