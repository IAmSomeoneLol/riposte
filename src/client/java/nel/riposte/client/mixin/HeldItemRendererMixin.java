package nel.riposte.client.mixin;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import nel.riposte.Riposte;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void riposte$applyFirstPersonAnimation(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {

        // 1. Only apply this to our Main Hand (Right Arm)
        if (hand == Hand.MAIN_HAND && player == MinecraftClient.getInstance().player) {

            var animationContainer = (ModifierLayer<dev.kosmx.playerAnim.api.layered.IAnimation>)
                    PlayerAnimationAccess.getPlayerAssociatedData(player).get(new Identifier(Riposte.MOD_ID, "animation"));

            // 2. Check if the Parry animation is actively playing
            if (animationContainer != null && animationContainer.isActive()) {

                Vec3f position = animationContainer.get3DTransform("right_arm", TransformType.POSITION, tickDelta, new Vec3f(0f, 0f, 0f));
                Vec3f rotation = animationContainer.get3DTransform("right_arm", TransformType.ROTATION, tickDelta, new Vec3f(0f, 0f, 0f));

                // 3. Shift the pivot point down to the shoulder
                matrices.translate(0.4f, -0.6f, -0.2f);

                // 4. Apply Blockbench Rotations
                matrices.multiply(RotationAxis.POSITIVE_Z.rotation(rotation.getZ()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotation.getY()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotation(rotation.getX()));

                // 5. Apply Blockbench Position Translations (Divided by 16 for scale)
                matrices.translate(position.getX() / 16f, position.getY() / 16f, position.getZ() / 16f);

                // 6. Return the pivot point back up to the camera
                matrices.translate(-0.4f, 0.6f, 0.2f);
            }
        }
    }
}