package nel.riposte.mixin;

import nel.riposte.ParryData;
import nel.riposte.Riposte;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements ParryData {

    @Unique
    private long parryTimestamp = 0L;

    @Unique
    private long successfulParryTimestamp = 0L;

    @Unique
    private long successfulComboTimestamp = 0L;

    @Unique
    private int lastParriedEntityId = -1;

    @Override
    public long getParryTimestamp() {
        return this.parryTimestamp;
    }

    @Override
    public void setParryTimestamp(long timestamp) {
        this.parryTimestamp = timestamp;
    }

    @Override
    public long getSuccessfulParryTimestamp() {
        return this.successfulParryTimestamp;
    }

    @Override
    public void setSuccessfulParryTimestamp(long timestamp) {
        this.successfulParryTimestamp = timestamp;
    }

    @Override
    public long getSuccessfulComboTimestamp() {
        return this.successfulComboTimestamp;
    }

    @Override
    public void setSuccessfulComboTimestamp(long timestamp) {
        this.successfulComboTimestamp = timestamp;
    }

    @Override
    public int getLastParriedEntityId() {
        return this.lastParriedEntityId;
    }

    @Override
    public void setLastParriedEntityId(int id) {
        this.lastParriedEntityId = id;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void riposte$airParryProjectiles(CallbackInfo ci) {
        if (!Riposte.CONFIG.deflectProjectiles) return;

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient) return;

        if (!this.canParryProjectiles()) return;

        int currentWindow = this.getCalculatedWindow(Riposte.CONFIG.parryWindowMs);
        if (this.isParryActive(currentWindow)) {

            Box parryBox = player.getBoundingBox().expand(1.5);
            List<ProjectileEntity> projectiles = player.getWorld().getEntitiesByClass(
                    ProjectileEntity.class,
                    parryBox,
                    p -> p.getOwner() != player
            );

            for (ProjectileEntity projectile : projectiles) {
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

                if (projectile instanceof ExplosiveProjectileEntity explosive) {
                    explosive.powerX = lookDir.x * 0.1;
                    explosive.powerY = lookDir.y * 0.1;
                    explosive.powerZ = lookDir.z * 0.1;
                }

                projectile.velocityModified = true;

                player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);

                if (player.getWorld() instanceof ServerWorld serverWorld) {
                    // Find the midpoint floating between the player and the projectile
                    double midX = (player.getX() + projectile.getX()) / 2.0;
                    double midY = (player.getEyeY() + projectile.getY()) / 2.0;
                    double midZ = (player.getZ() + projectile.getZ()) / 2.0;

                    // Perfect 3D spherical spark burst using spark_0 through spark_7
                    serverWorld.spawnParticles(ParticleTypes.FIREWORK, midX, midY, midZ, 15, 0.0, 0.0, 0.0, 0.15);

                    ItemStack mainHand = player.getMainHandStack();
                    Item item = mainHand.getItem();
                    boolean isWeapon = item instanceof SwordItem || item instanceof MiningToolItem || item instanceof TridentItem;

                    if (isWeapon) {
                        serverWorld.spawnParticles(ParticleTypes.SWEEP_ATTACK, midX, midY, midZ, 1, 0, 0, 0, 0);
                    }
                }

                this.setSuccessfulParryTimestamp(System.currentTimeMillis());
                this.setLastParriedEntityId(projectile.getId());

                var capability = io.wispforest.accessories.api.AccessoriesCapability.get(player);
                if (capability != null && capability.isEquipped(Riposte.WANDERERS_CAPE)) {
                    this.refundParryCooldown(Riposte.CONFIG.wanderersCapeCooldownCharge);
                }

                if (player instanceof ServerPlayerEntity serverPlayer) {
                    ServerPlayNetworking.send(serverPlayer, Riposte.PARRY_SUCCESS_PACKET, PacketByteBufs.create());
                }
            }
        }
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void riposte$onAttack(Entity target, CallbackInfo ci) {
        if (Riposte.CONFIG.kickCombo && target instanceof LivingEntity livingTarget) {
            if (target.getId() == this.lastParriedEntityId) {
                long timeSinceParry = System.currentTimeMillis() - this.successfulParryTimestamp;

                if (timeSinceParry <= Riposte.CONFIG.kickComboWindowMs) {
                    PlayerEntity player = (PlayerEntity) (Object) this;
                    double heavyKnockback = Riposte.CONFIG.parryKnockback * Riposte.CONFIG.kickComboKnockbackMultiplier;

                    this.applyParryKnockback(livingTarget, heavyKnockback, player.getX() - livingTarget.getX(), player.getZ() - livingTarget.getZ());
                    this.lastParriedEntityId = -1;

                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        ServerPlayNetworking.send(serverPlayer, Riposte.COMBO_SUCCESS_PACKET, PacketByteBufs.create());
                    }
                }
            }
        }
    }
}