package nel.riposte.client.mixin;

import nel.riposte.FinisherData;
import nel.riposte.client.RiposteClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void riposte$smoothCameraPan(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        FinisherData fData = (FinisherData) client.player;
        boolean isExecuting = fData.isExecutingFinisher();
        long now = System.currentTimeMillis();
        float durationMs = RiposteClient.CLIENT_CONFIG.addons.finishers.cameraCenterPanningDurationMs;
        if (durationMs <= 0) durationMs = 1;

        if (isExecuting && RiposteClient.CLIENT_CONFIG.addons.finishers.cameraLock) {
            ci.cancel();

            if (RiposteClient.finisherStartTime == 0) return;

            Entity target = client.world.getEntityById(fData.getFinisherTargetId());
            if (target != null) {
                float tickDelta = client.getTickDelta();

                // AIM DIRECTLY AT THE HEAD
                double targetX = MathHelper.lerp(tickDelta, target.prevX, target.getX());
                double targetY = MathHelper.lerp(tickDelta, target.prevY, target.getY()) + target.getEyeHeight(target.getPose());
                double targetZ = MathHelper.lerp(tickDelta, target.prevZ, target.getZ());

                double playerX = MathHelper.lerp(tickDelta, client.player.prevX, client.player.getX());
                double playerY = MathHelper.lerp(tickDelta, client.player.prevY, client.player.getY()) + client.player.getEyeHeight(client.player.getPose());
                double playerZ = MathHelper.lerp(tickDelta, client.player.prevZ, client.player.getZ());

                double dx = targetX - playerX;
                double dy = targetY - playerY;
                double dz = targetZ - playerZ;
                double dh = Math.sqrt(dx * dx + dz * dz);

                float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float targetPitch = (float) Math.toDegrees(Math.atan2(-dy, dh));

                long elapsed = now - RiposteClient.finisherStartTime;
                float t = MathHelper.clamp((float) elapsed / durationMs, 0f, 1f);

                // QUINTIC EASING - Extremely smooth glide into position
                float ease = t * t * t * (t * (t * 6 - 15) + 10);

                float newYaw = MathHelper.lerpAngleDegrees(ease, RiposteClient.originalYaw, targetYaw);
                float newPitch = MathHelper.lerp(ease, RiposteClient.originalPitch, targetPitch);

                client.player.setYaw(newYaw);
                client.player.setPitch(newPitch);
                client.player.prevYaw = newYaw;
                client.player.prevPitch = newPitch;
            }

        } else if (!isExecuting && RiposteClient.finisherEndTime > 0 && RiposteClient.CLIENT_CONFIG.addons.finishers.cameraLock) {

            long elapsed = now - RiposteClient.finisherEndTime;
            if (elapsed <= durationMs) {
                ci.cancel();

                float t = MathHelper.clamp((float) elapsed / durationMs, 0f, 1f);
                float ease = t * t * t * (t * (t * 6 - 15) + 10);

                float newYaw = MathHelper.lerpAngleDegrees(ease, RiposteClient.lockedYaw, RiposteClient.originalYaw);
                float newPitch = MathHelper.lerp(ease, RiposteClient.lockedPitch, RiposteClient.originalPitch);

                client.player.setYaw(newYaw);
                client.player.setPitch(newPitch);
                client.player.prevYaw = newYaw;
                client.player.prevPitch = newPitch;
            } else {
                RiposteClient.finisherEndTime = 0;
            }
        }
    }
}