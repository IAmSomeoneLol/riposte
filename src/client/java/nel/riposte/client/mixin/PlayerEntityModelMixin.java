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
    private void riposte$hideBodyInFirstPerson(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {

        if (entity == MinecraftClient.getInstance().player && MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {

            var animationContainer = PlayerAnimationAccess.getPlayerAssociatedData((AbstractClientPlayerEntity) entity).get(new Identifier(Riposte.MOD_ID, "animation"));

            if (animationContainer != null && animationContainer.isActive()) {
                PlayerEntityModel<?> model = (PlayerEntityModel<?>) (Object) this;

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
            }
        }
    }
}