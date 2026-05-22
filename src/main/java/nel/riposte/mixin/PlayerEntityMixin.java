package nel.riposte.mixin;

import nel.riposte.ParryData;
import nel.riposte.Riposte;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
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

            // NEW: MULTI-PARRY CONSUMPTION CHECK
            if (!Riposte.CONFIG.allowMultiParry && this.successfulParryTimestamp >= this.parryTimestamp) {
                return; // Parry was already used up!
            }

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

                ItemStack mainHand = player.getMainHandStack();
                Item item = mainHand.getItem();
                boolean isWeapon = item instanceof SwordItem || item instanceof MiningToolItem || item instanceof TridentItem;

                float randomPitch = 1.0f + (player.getWorld().random.nextFloat() - 0.5f) * 0.6f;
                SoundEvent soundToPlay = isWeapon ? Riposte.WEAPON_PARRY_SOUND : Riposte.NORMAL_PARRY_SOUND;

                player.getWorld().playSound(null, player.getBlockPos(), soundToPlay, SoundCategory.PLAYERS, 1.0f, randomPitch);

                if (!player.getWorld().isClient) {
                    double midX = player.getX() + (lookDir.x * 1.2);
                    double midY = player.getEyeY() + (lookDir.y * 1.2) - 0.2;
                    double midZ = player.getZ() + (lookDir.z * 1.2);

                    for (ServerPlayerEntity tracker : PlayerLookup.tracking(player)) {
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeDouble(midX);
                        buf.writeDouble(midY);
                        buf.writeDouble(midZ);
                        buf.writeFloat(player.getYaw());
                        buf.writeBoolean(isWeapon);
                        ServerPlayNetworking.send(tracker, Riposte.PARRY_VFX_PACKET, buf);
                    }
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeDouble(midX);
                        buf.writeDouble(midY);
                        buf.writeDouble(midZ);
                        buf.writeFloat(player.getYaw());
                        buf.writeBoolean(isWeapon);
                        ServerPlayNetworking.send(serverPlayer, Riposte.PARRY_VFX_PACKET, buf);
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

                    float randomPitch = 1.0f + (player.getWorld().random.nextFloat() - 0.5f) * 0.4f;
                    player.getWorld().playSound(null, player.getBlockPos(), Riposte.KICK_COMBO_SOUND, SoundCategory.PLAYERS, 1.0f, randomPitch);

                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        ServerPlayNetworking.send(serverPlayer, Riposte.COMBO_SUCCESS_PACKET, PacketByteBufs.create());
                    }
                }
            }
        }
    }
}