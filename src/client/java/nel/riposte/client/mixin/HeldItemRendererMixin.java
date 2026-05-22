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
public class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void riposte$renderCustomViewmodel(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (player == MinecraftClient.getInstance().player && hand == Hand.MAIN_HAND) {
            var animationContainer = PlayerAnimationAccess.getPlayerAssociatedData(player)
                    .get(new Identifier(Riposte.MOD_ID, "animation"));

            if (animationContainer != null && animationContainer.isActive()) {
                // Get the rotation from your JSON bone named "right_arm"
                Vec3f rotation = animationContainer.get3DTransform("right_arm", TransformType.ROTATION, tickDelta, new Vec3f(0, 0, 0));
                Vec3f position = animationContainer.get3DTransform("right_arm", TransformType.POSITION, tickDelta, new Vec3f(0, 0, 0));

                // 1. Move the pivot to the shoulder (approximate offset)
                matrices.translate(0.0, -0.5, 0.0);

                // 2. Apply your Blockbench Rotations
                matrices.multiply(RotationAxis.POSITIVE_Z.rotation(rotation.getZ()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotation.getY()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotation(rotation.getX()));

                // 3. Apply position
                matrices.translate(position.getX() / 16.0, position.getY() / 16.0, position.getZ() / 16.0);
            }
        }
    }
}