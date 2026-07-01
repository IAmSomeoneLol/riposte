package nel.riposte.client.mixin;

import nel.riposte.FinisherData;
import nel.riposte.ParryData;
import nel.riposte.client.RiposteClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    // We will use these later to dynamically shake the screen based on the JSON
    @Unique private float riposte$finisherShake = 0.0f;
    @Unique private float riposte$finisherPitch = 0.0f;

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

        // --- 1. EXISTING PARRY RECOIL & CAMERA SHAKE ---
        if (RiposteClient.CLIENT_CONFIG.cameraRecoil && timeSince < RiposteClient.CLIENT_CONFIG.recoilDurationMs) {
            float progress = (float) timeSince / RiposteClient.CLIENT_CONFIG.recoilDurationMs;
            float ease = (float) Math.pow(1.0 - progress, 3);

            if (RiposteClient.lastParryWasFall) {
                finalPitchOffset += RiposteClient.CLIENT_CONFIG.recoilIntensity * ease;
            } else {
                Random rand = new Random(data.getSuccessfulParryTimestamp());
                double angle = rand.nextDouble() * Math.PI * 2.0;

                float yawDir = (float) Math.cos(angle);
                float pitchDir = (float) Math.sin(angle);

                finalYawOffset += yawDir * RiposteClient.CLIENT_CONFIG.recoilIntensity * ease;
                finalPitchOffset += pitchDir * RiposteClient.CLIENT_CONFIG.recoilIntensity * ease;
            }
        }

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

        if (RiposteClient.CLIENT_CONFIG.parryFallCameraFlow && RiposteClient.lastParryWasFall && timeSince < 1000) {
            float progress = (float) timeSince / 1000f;
            float ease = (float) Math.pow(1.0 - progress, 3);
            upMove -= 0.75f * ease;
        }

        // --- 2. NEW: DATA-DRIVEN FINISHER CAMERA MODIFIERS ---
        FinisherData fData = (FinisherData) client.player;
        if (!thirdPerson && fData.isExecutingFinisher()) {

            // NOTE: We will inject the JSON timeline offsets into these variables in the next step!
            // For now, this logic safely coexists alongside your Parry camera logic.
            if (this.riposte$finisherShake > 0) {
                finalYawOffset += (float) (Math.random() - 0.5) * this.riposte$finisherShake;
                finalPitchOffset += (float) (Math.random() - 0.5) * this.riposte$finisherShake;

                this.riposte$finisherShake *= 0.8f; // Decays shake over time
            }

            finalPitchOffset += this.riposte$finisherPitch;
            this.riposte$finisherPitch *= 0.9f; // Decays pitch over time
        } else {
            this.riposte$finisherShake = 0f;
            this.riposte$finisherPitch = 0f;
        }

        // --- 3. APPLY ALL TRANSFORMATIONS ---
        this.moveBy(forwardMove, upMove, rightMove);
        this.setRotation(this.yaw + finalYawOffset, this.pitch + finalPitchOffset);
    }
}