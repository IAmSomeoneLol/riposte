package nel.riposte.client.mixin;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import nel.riposte.*;
import nel.riposte.client.RiposteClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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

    @Unique private float riposte$finisherShake = 0.0f;
    @Unique private float riposte$finisherPitch = 0.0f;
    @Unique private float riposte$finisherYaw = 0.0f;

    @Unique private float riposte$finisherMoveX = 0.0f;
    @Unique private float riposte$finisherMoveY = 0.0f;
    @Unique private float riposte$finisherMoveZ = 0.0f;

    @Unique private float riposte$smoothAnimPitch = 0.0f;
    @Unique private float riposte$smoothAnimYaw = 0.0f;

    @Inject(method = "update", at = @At("TAIL"))
    private void riposte$applyEssenceOfParryCamera(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ParryData data = (ParryData) client.player;
        long timeSinceParry = System.currentTimeMillis() - data.getSuccessfulParryTimestamp();

        // --- NEW: MERGED RECOIL ---
        long timeSinceFinisherRecoil = System.currentTimeMillis() - RiposteClient.finisherRecoilTimestamp;
        long timeSince = Math.min(timeSinceParry, timeSinceFinisherRecoil);

        float finalPitchOffset = 0f;
        float finalYawOffset = 0f;

        float forwardMove = 0f;
        float upMove = 0f;
        float rightMove = 0f;

        if (RiposteClient.CLIENT_CONFIG.cameraRecoil && timeSince < RiposteClient.CLIENT_CONFIG.recoilDurationMs) {
            float progress = (float) timeSince / RiposteClient.CLIENT_CONFIG.recoilDurationMs;
            float ease = (float) Math.pow(1.0 - progress, 3);

            if (RiposteClient.lastParryWasFall && timeSince == timeSinceParry) {
                finalPitchOffset += RiposteClient.CLIENT_CONFIG.recoilIntensity * ease;
            } else {
                Random rand = new Random(timeSince == timeSinceParry ? data.getSuccessfulParryTimestamp() : RiposteClient.finisherRecoilTimestamp);
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

        if (RiposteClient.CLIENT_CONFIG.parryFallCameraFlow && RiposteClient.lastParryWasFall && timeSince == timeSinceParry && timeSince < 1000) {
            float progress = (float) timeSince / 1000f;
            float ease = (float) Math.pow(1.0 - progress, 3);
            upMove -= 0.75f * ease;
        }

        if (!thirdPerson && focusedEntity instanceof AbstractClientPlayerEntity player) {
            FinisherData fData = (FinisherData) player;
            if (fData.isExecutingFinisher()) {

                FinisherDefinition def = FinisherLoader.getFinisherById(fData.getActiveFinisherId());

                if (def != null && !def.disable_head_tracking) {
                    var animationContainer = PlayerAnimationAccess.getPlayerAssociatedData(player).get(new Identifier(Riposte.MOD_ID, "animation"));
                    if (animationContainer != null && animationContainer.isActive()) {
                        Vec3f headRot = animationContainer.get3DTransform("head", TransformType.ROTATION, tickDelta, new Vec3f(0, 0, 0));

                        float targetAnimPitch = (float) Math.toDegrees(headRot.getX());
                        float targetAnimYaw = (float) Math.toDegrees(headRot.getY());

                        this.riposte$smoothAnimPitch = MathHelper.lerpAngleDegrees(0.08f, this.riposte$smoothAnimPitch, targetAnimPitch);
                        this.riposte$smoothAnimYaw = MathHelper.lerpAngleDegrees(0.08f, this.riposte$smoothAnimYaw, targetAnimYaw);

                        finalPitchOffset += this.riposte$smoothAnimPitch;
                        finalYawOffset -= this.riposte$smoothAnimYaw;
                    }
                }

                if (def != null && def.timeline != null) {
                    for (FinisherDefinition.TimelineEvent event : def.timeline) {
                        if (event.tick == fData.getFinisherTick()) {
                            if ("camera_shake".equals(event.action)) this.riposte$finisherShake += event.amount;
                            if ("camera_pitch".equals(event.action)) this.riposte$finisherPitch += event.amount;
                            if ("camera_yaw".equals(event.action)) this.riposte$finisherYaw += event.amount;
                            if ("camera_move".equals(event.action)) {
                                this.riposte$finisherMoveX += event.x_offset;
                                this.riposte$finisherMoveY += event.y_offset;
                                this.riposte$finisherMoveZ += event.z_offset;
                            }
                        }
                    }
                }

                if (this.riposte$finisherShake > 0) {
                    finalYawOffset += (float) (Math.random() - 0.5) * this.riposte$finisherShake;
                    finalPitchOffset += (float) (Math.random() - 0.5) * this.riposte$finisherShake;
                    this.riposte$finisherShake *= 0.8f;
                }

                finalPitchOffset += this.riposte$finisherPitch;
                finalYawOffset += this.riposte$finisherYaw;
                this.riposte$finisherPitch *= 0.9f;
                this.riposte$finisherYaw *= 0.9f;

                rightMove += this.riposte$finisherMoveX;
                upMove += this.riposte$finisherMoveY;
                forwardMove += this.riposte$finisherMoveZ;

                this.riposte$finisherMoveX *= 0.85f;
                this.riposte$finisherMoveY *= 0.85f;
                this.riposte$finisherMoveZ *= 0.85f;

            } else {
                this.riposte$smoothAnimPitch = 0f;
                this.riposte$smoothAnimYaw = 0f;
                this.riposte$finisherShake = 0f;
                this.riposte$finisherPitch = 0f;
                this.riposte$finisherYaw = 0f;
                this.riposte$finisherMoveX = 0f;
                this.riposte$finisherMoveY = 0f;
                this.riposte$finisherMoveZ = 0f;
            }
        }

        this.moveBy(forwardMove, upMove, rightMove);
        this.setRotation(this.yaw + finalYawOffset, this.pitch + finalPitchOffset);
    }
}