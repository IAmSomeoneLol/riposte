package nel.riposte.mixin;

import io.wispforest.accessories.api.AccessoriesCapability;
import nel.riposte.HitstopData;
import nel.riposte.ParryData;
import nel.riposte.Riposte;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements HitstopData {

    @Unique
    private int riposte$hitstopTicks = 0;

    @Override
    public void setHitstop(int ticks) {
        this.riposte$hitstopTicks = ticks;
    }

    @Override
    public int getHitstop() {
        return this.riposte$hitstopTicks;
    }

    @Inject(method = "tickMovement", at = @At("HEAD"), cancellable = true)
    private void riposte$onTickMovement(CallbackInfo ci) {
        if (this.riposte$hitstopTicks > 0) {
            this.riposte$hitstopTicks--;
            ci.cancel();
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void riposte$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            ParryData parryData = (ParryData) player;

            // Ask the Brain if this damage is legal
            if (!parryData.canParryDamageType(source)) return;

            // Ask the Brain for our dynamically calculated window
            int currentWindow = parryData.getCalculatedWindow(Riposte.CONFIG.parryWindowMs);

            if (parryData.isParryActive(currentWindow)) {
                cir.setReturnValue(false); // Deny the damage

                // --- PROJECTILE PARRY LOGIC ---
                if (source.getSource() instanceof ProjectileEntity projectile) {
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);

                    if (Riposte.CONFIG.deflectProjectiles) {
                        Entity owner = projectile.getOwner();

                        // CRITICAL FIX: Transfer ownership to the player.
                        // This stops the arrow from hurting you, and allows it to hurt the enemy!
                        projectile.setOwner(player);

                        if (owner != null) {
                            // Calculate trajectory from the player's eyes to the enemy's eyes
                            Vec3d dir = owner.getEyePos().subtract(player.getEyePos()).normalize();
                            // Fire it back slightly faster (3.0f) so it feels like a real deflection
                            projectile.setVelocity(dir.x, dir.y, dir.z, 3.0f, 0.0f);
                        } else {
                            // If no owner, shoot exactly where the player is currently looking
                            Vec3d lookDir = player.getRotationVector();
                            projectile.setVelocity(lookDir.x, lookDir.y, lookDir.z, 3.0f, 0.0f);
                        }
                        projectile.velocityModified = true;
                    }
                    return; // End sequence early to prevent melee knockback/hitstop
                }

                // --- MELEE PARRY LOGIC ---
                ItemStack mainHand = player.getMainHandStack();
                Item item = mainHand.getItem();
                boolean isWeapon = item instanceof SwordItem || item instanceof MiningToolItem || item instanceof TridentItem;

                if (isWeapon) {
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1.0f, 1.0f);
                } else {
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }

                if (source.getAttacker() instanceof LivingEntity attacker) {

                    parryData.setSuccessfulParryTimestamp(System.currentTimeMillis());
                    parryData.setLastParriedEntityId(attacker.getId());

                    boolean isBoss = attacker instanceof WitherEntity ||
                            attacker instanceof EnderDragonEntity ||
                            attacker instanceof WardenEntity ||
                            attacker instanceof ElderGuardianEntity;

                    if (isBoss) {
                        parryData.applyParryKnockback(attacker, Riposte.CONFIG.parryKnockback, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
                        parryData.applyParryKnockback(player, Riposte.CONFIG.parryKnockback, attacker.getX() - player.getX(), attacker.getZ() - player.getZ());
                    } else {
                        parryData.applyParryKnockback(attacker, Riposte.CONFIG.parryKnockback, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
                    }

                    if (Riposte.CONFIG.hitstopEnabled) {
                        int hitstopFrames = Math.max(1, Riposte.CONFIG.hitstopMs / 50);
                        this.setHitstop(hitstopFrames);
                        ((HitstopData) attacker).setHitstop(hitstopFrames);
                    }
                }
            }
        }
    }
}