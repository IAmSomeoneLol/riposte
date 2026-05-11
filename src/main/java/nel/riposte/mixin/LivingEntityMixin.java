package nel.riposte.mixin;

import nel.riposte.HitstopData;
import nel.riposte.ParryData;
import nel.riposte.Riposte;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

    @Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
    private void riposte$cobaltPlateKnockback(double strength, double x, double z, CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player) {
            var capability = io.wispforest.accessories.api.AccessoriesCapability.get(player);
            if (capability != null && capability.isEquipped(Riposte.COBALT_PLATE)) {
                ci.cancel();
            }
        }
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float riposte$accessoryDamageModifiers(float amount, DamageSource source) {
        float modifiedAmount = amount;

        if (source.getAttacker() instanceof PlayerEntity attacker) {
            var capability = io.wispforest.accessories.api.AccessoriesCapability.get(attacker);
            if (capability != null && capability.isEquipped(Riposte.WANDERERS_CAPE)) {
                modifiedAmount *= 1.1f;
            }
        }

        if ((Object) this instanceof PlayerEntity victim) {
            var capability = io.wispforest.accessories.api.AccessoriesCapability.get(victim);
            if (capability != null && capability.isEquipped(Riposte.EVERLASTING_BLOODRING)) {
                modifiedAmount *= 1.5f;
            }
        }

        return modifiedAmount;
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void riposte$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            ParryData parryData = (ParryData) player;

            if (!parryData.canParryDamageType(source)) return;

            int currentWindow = parryData.getCalculatedWindow(Riposte.CONFIG.parryWindowMs);

            if (parryData.isParryActive(currentWindow)) {
                cir.setReturnValue(false);

                if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                    return;
                }

                Entity rawAttacker = source.getAttacker();

                if (rawAttacker != null) {
                    long timeSinceLastParry = System.currentTimeMillis() - parryData.getSuccessfulParryTimestamp();

                    if (rawAttacker.getId() == parryData.getLastParriedEntityId() && timeSinceLastParry <= currentWindow) {
                        return;
                    }
                }

                ItemStack mainHand = player.getMainHandStack();
                Item item = mainHand.getItem();
                boolean isWeapon = item instanceof SwordItem || item instanceof MiningToolItem || item instanceof TridentItem;

                if (isWeapon) {
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1.0f, 1.0f);
                } else {
                    player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);
                }

                if (player.getWorld() instanceof ServerWorld serverWorld) {
                    double midX, midY, midZ;

                    // Calculate the exact 3D midpoint between the player and the attacker
                    if (rawAttacker != null) {
                        midX = (player.getX() + rawAttacker.getX()) / 2.0;
                        midY = (player.getEyeY() + (rawAttacker.getY() + rawAttacker.getHeight() / 2.0)) / 2.0;
                        midZ = (player.getZ() + rawAttacker.getZ()) / 2.0;
                    } else {
                        // Fallback: Just spawn it 1 block in front of the player's face
                        Vec3d look = player.getRotationVector();
                        midX = player.getX() + look.x;
                        midY = player.getEyeY() + look.y;
                        midZ = player.getZ() + look.z;
                    }

                    // Setting delta to 0.0 with speed 0.15 makes them explode outwards in a perfect 3D sphere
                    // FIREWORK uses the exact spark_0 through spark_7 texture animation!
                    serverWorld.spawnParticles(ParticleTypes.FIREWORK, midX, midY, midZ, 15, 0.0, 0.0, 0.0, 0.15);

                    // Only spawns the horizontal sweep slash if holding a weapon
                    if (isWeapon) {
                        serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, midX, midY, midZ, 1, 0, 0, 0, 0);
                    }
                }

                parryData.setSuccessfulParryTimestamp(System.currentTimeMillis());

                if (player instanceof ServerPlayerEntity serverPlayer) {
                    ServerPlayNetworking.send(serverPlayer, Riposte.PARRY_SUCCESS_PACKET, PacketByteBufs.create());
                }

                var capability = io.wispforest.accessories.api.AccessoriesCapability.get(player);
                if (capability != null && capability.isEquipped(Riposte.WANDERERS_CAPE)) {
                    parryData.refundParryCooldown(Riposte.CONFIG.wanderersCapeCooldownCharge);
                }

                if (rawAttacker instanceof LivingEntity attacker) {
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

                    if (Riposte.CONFIG.hitstop) {
                        int hitstopFrames = Math.max(1, Riposte.CONFIG.hitstopMs / 50);
                        this.setHitstop(hitstopFrames);
                        ((HitstopData) attacker).setHitstop(hitstopFrames);
                    }
                } else {
                    parryData.setLastParriedEntityId(-1);
                }
            }
        }
    }
}