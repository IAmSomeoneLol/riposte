package nel.riposte.mixin;

import nel.riposte.ParryData;
import nel.riposte.Riposte;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void riposte$hijackProjectile(HitResult hitResult, CallbackInfo ci) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            Entity hitEntity = ((EntityHitResult) hitResult).getEntity();

            if (hitEntity instanceof PlayerEntity player) {
                ParryData parryData = (ParryData) player;
                int currentWindow = parryData.getCalculatedWindow(Riposte.CONFIG.parryWindowMs);

                if (parryData.isParryActive(currentWindow) && parryData.canParryProjectiles()) {

                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);

                    if (Riposte.CONFIG.deflectProjectiles) {
                        ProjectileEntity projectile = (ProjectileEntity) (Object) this;
                        Vec3d lookDir = player.getRotationVector();

                        double speed = Math.max(projectile.getVelocity().length(), 1.5);

                        projectile.setOwner(player);

                        projectile.setPosition(
                                player.getX() + (lookDir.x * 0.5),
                                player.getEyeY() + (lookDir.y * 0.5),
                                player.getZ() + (lookDir.z * 0.5)
                        );

                        if (projectile instanceof PersistentProjectileEntity arrow) {
                            ((PersistentProjectileEntityAccessor) arrow).setInGround(false);
                            arrow.setVelocity(lookDir.x, lookDir.y, lookDir.z, 3.0F, 0.0F);
                        } else {
                            projectile.setVelocity(lookDir.x, lookDir.y, lookDir.z, (float) speed * 1.5f, 0.0f);
                        }

                        // Update the internal motor for Fireballs and Wither Skulls
                        if (projectile instanceof ExplosiveProjectileEntity explosive) {
                            explosive.powerX = lookDir.x * 0.1;
                            explosive.powerY = lookDir.y * 0.1;
                            explosive.powerZ = lookDir.z * 0.1;
                        }

                        projectile.velocityModified = true;
                    }

                    ci.cancel();
                }
            }
        }
    }
}