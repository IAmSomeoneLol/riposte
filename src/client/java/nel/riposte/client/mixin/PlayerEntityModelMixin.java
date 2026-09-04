package nel.riposte.client.mixin;

import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import nel.riposte.Riposte;
import nel.riposte.client.RiposteClient;
import nel.riposte.client.compat.FPMCompat;
import nel.riposte.client.render.AnimationBlender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerEntityModel.class, priority = 10000)
public class PlayerEntityModelMixin {

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void riposte$handleParryAnimations(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {

        if (entity instanceof AbstractClientPlayerEntity player) {
            var animationContainer = PlayerAnimationAccess.getPlayerAssociatedData(player).get(Identifier.of(Riposte.MOD_ID, "animation"));
            PlayerEntityModel<?> model = (PlayerEntityModel<?>) (Object) this;

            MinecraftClient client = MinecraftClient.getInstance();
            boolean isFirstPerson = player == client.player && client.options.getPerspective().isFirstPerson();
            boolean inInventory = client.currentScreen != null;
            boolean isAnimationActive = animationContainer != null && animationContainer.isActive();

            boolean isFpm = FPMCompat.isFpmEnabled();

            AnimationBlender.PlayerBlendState state = AnimationBlender.get(player.getUuid());

            if (isAnimationActive) {
                boolean isFallDamage = "parry_fall_damage".equals(RiposteClient.currentParryAnimation);

                float pitchRad = headPitch * ((float)Math.PI / 180F);

                if (!isFallDamage) {
                    model.rightArm.pitch += pitchRad;
                    model.rightSleeve.pitch += pitchRad;

                    if (RiposteClient.renderLeftArm) {
                        model.leftArm.pitch += pitchRad;
                        model.leftSleeve.pitch += pitchRad;
                    }
                }

                if (isFirstPerson && !isFpm && !inInventory) {
                    model.head.visible = false;
                    model.hat.visible = false;
                    model.body.visible = false;
                    model.jacket.visible = false;
                    model.leftArm.visible = RiposteClient.renderLeftArm;
                    model.leftSleeve.visible = RiposteClient.renderLeftArm;
                    model.leftLeg.visible = false;
                    model.leftPants.visible = false;
                    model.rightLeg.visible = false;
                    model.rightPants.visible = false;
                } else if (!isFallDamage) {
                    model.body.pitch += pitchRad * 0.5f;
                    model.jacket.pitch += pitchRad * 0.5f;
                }
            } else {
                RiposteClient.currentParryAnimation = "";

                // Visibility
                if (!isFirstPerson || inInventory) {
                    model.head.visible = true;
                    model.hat.visible = true;
                    model.body.visible = true;
                    model.jacket.visible = true;
                    model.leftArm.visible = true;
                    model.leftSleeve.visible = true;
                    model.rightArm.visible = true;
                    model.rightSleeve.visible = true;
                    model.leftLeg.visible = true;
                    model.leftPants.visible = true;
                    model.rightLeg.visible = true;
                    model.rightPants.visible = true;
                }
            }

            // Blender
            long now = System.currentTimeMillis();

            if (isAnimationActive && !state.wasActive) {
                state.blendStart.copyFrom(state.lastFrame);
                state.blendStartTime = now;
            } else if (!isAnimationActive && state.wasActive) {
                state.blendStart.copyFrom(state.lastFrame);
                state.blendStartTime = now;
            }
            state.wasActive = isAnimationActive;

            long elapsed = now - state.blendStartTime;
            long blendDurationMs = 250;

            if (elapsed < blendDurationMs) {
                float progress = (float) elapsed / blendDurationMs;
                float ease = (float) (0.5 * (1.0 - Math.cos(Math.PI * progress)));
                state.applyBlend(model, ease);
            }
            state.lastFrame.copyFrom(model);
        }
    }
}