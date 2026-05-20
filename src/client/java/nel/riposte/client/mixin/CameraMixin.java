package nel.riposte.client.mixin;

import nel.riposte.client.RiposteClient;
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

        // 1. Apply physical pushback and lateral walk.
        // We strictly only apply this in first-person so the camera doesn't clip through walls in third-person!
        if (!thirdPerson) {
            this.moveBy(RiposteClient.currentCameraXOffset, RiposteClient.currentCameraYOffset, RiposteClient.currentCameraZOffset);
        }

        // 2. Apply the visual rotation shake.
        // By modifying rotation here, the screen twists violently but your actual crosshair aim isn't permanently altered.
        this.setRotation(this.yaw + RiposteClient.currentYawOffset, this.pitch + RiposteClient.currentPitchOffset);
    }
}