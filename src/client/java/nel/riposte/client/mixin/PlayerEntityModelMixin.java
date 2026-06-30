package nel.riposte.client.mixin;

import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import nel.riposte.Riposte;
import nel.riposte.client.RiposteClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin {

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void riposte$handleParryAnimations(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {

        if (entity instanceof AbstractClientPlayerEntity player) {
            var animationContainer = PlayerAnimationAccess.getPlayerAssociatedData(player).get(new Identifier(Riposte.MOD_ID, "animation"));

            if (animationContainer != null && animationContainer.isActive()) {
                PlayerEntityModel<?> model = (PlayerEntityModel<?>) (Object) this;
                boolean isFirstPerson = player == MinecraftClient.getInstance().player && MinecraftClient.getInstance().options.getPerspective().isFirstPerson();

                float pitchRad = headPitch * ((float)Math.PI / 180F);

                model.rightArm.pitch += pitchRad;
                model.rightSleeve.pitch += pitchRad;

                if (RiposteClient.renderLeftArm) {
                    model.leftArm.pitch += pitchRad;
                    model.leftSleeve.pitch += pitchRad;
                }

                if (isFirstPerson) {

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
                } else {
                    // Bend the spine slightly (50%) ONLY in Third-Person for a natural looking posture
                    model.body.pitch += pitchRad * 0.5f;
                    model.jacket.pitch += pitchRad * 0.5f;
                }
            }
        }
    }
}