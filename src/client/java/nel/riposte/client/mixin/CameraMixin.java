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
                // NEW: If it's a fall parry, apply the recoil directly into the downward Pitch.
                // In Minecraft, positive pitch makes you look down at the floor!
                finalPitchOffset += RiposteClient.CLIENT_CONFIG.recoilIntensity * ease;
            } else {
                // Regular parries still randomize the recoil direction
                Random rand = new Random(data.getSuccessfulParryTimestamp());
                double angle = rand.nextDouble() * Math.PI * 2.0;

                float yawDir = (float) Math.cos(angle);
                float pitchDir = (float) Math.sin(angle);

                finalYawOffset += yawDir * RiposteClient.CLIENT_CONFIG.recoilIntensity * ease;
                finalPitchOffset += pitchDir * RiposteClient.CLIENT_CONFIG.recoilIntensity * ease;
            }
        }

        // We completely ignore the micro-screen shake for the fall parry so it's a smooth impact
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

        this.moveBy(forwardMove, upMove, rightMove);
        this.setRotation(this.yaw + finalYawOffset, this.pitch + finalPitchOffset);
    }
}