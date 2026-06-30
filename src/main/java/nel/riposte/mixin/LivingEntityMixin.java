package nel.riposte.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import nel.riposte.HitstopData;
import nel.riposte.ParryData;
import nel.riposte.Riposte;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
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
    private void riposte$copperGuardKnockback(double strength, double x, double z, CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player) {
            var component = TrinketsApi.getTrinketComponent(player).orElse(null);
            if (component != null && (component.isEquipped(Riposte.COPPER_GUARD) || component.isEquipped(Riposte.VOID_GUARD))) {
                ci.cancel();
            }
        }
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float riposte$accessoryDamageModifiers(float amount, DamageSource source) {
        float modifiedAmount = amount;

        if (source.getAttacker() instanceof PlayerEntity attacker) {
            var component = TrinketsApi.getTrinketComponent(attacker).orElse(null);
            if (component != null && component.isEquipped(Riposte.HONORABLE_CAPE)) {
                modifiedAmount *= 1.1f;
            }

            if (component != null && component.isEquipped(Riposte.BLOODLUSTFUL_RING)) {
                ParryData parryData = (ParryData) attacker;
                if (((Object) this instanceof LivingEntity victim) && victim.getId() == parryData.getLastParriedEntityId()) {
                    long timeSinceParry = System.currentTimeMillis() - parryData.getSuccessfulParryTimestamp();
                    if (timeSinceParry <= Riposte.CONFIG.kickComboWindowMs) {
                        modifiedAmount *= 1.6f;
                    }
                }
            }
        }

        if ((Object) this instanceof PlayerEntity victim) {
            var component = TrinketsApi.getTrinketComponent(victim).orElse(null);
            if (component != null && component.isEquipped(Riposte.BLOODLUSTFUL_RING)) {
                modifiedAmount *= 1.5f;
            }
        }

        return modifiedAmount;
    }

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void riposte$blockDebuffsDuringParry(StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            ParryData parryData = (ParryData) player;
            int currentWindow = parryData.getCalculatedWindow(Riposte.CONFIG.parryWindowMs);

            if (effect != null && effect.getEffectType().getCategory() == StatusEffectCategory.HARMFUL) {
                if (parryData.isParryActive(currentWindow)) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void riposte$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player) {
            ParryData parryData = (ParryData) player;

            if (!parryData.canParryDamageType(source)) return;

            int currentWindow = parryData.getCalculatedWindow(Riposte.CONFIG.parryWindowMs);

            if (source.isOf(DamageTypes.FALL) && !parryData.isParryActive(currentWindow)) {
                var component = TrinketsApi.getTrinketComponent(player).orElse(null);
                int neuralCount = component != null ? component.getEquipped(Riposte.NEURAL_LINK).size() : 0;

                if (neuralCount > 0 && player.getWorld().getRandom().nextFloat() < (0.50f * neuralCount)) {
                    int cooldown = parryData.getCalculatedCooldown(Riposte.CONFIG.parryCooldownMs);
                    if (parryData.canParry(cooldown)) {
                        parryData.setParryTimestamp(System.currentTimeMillis());
                    }
                }
            }

            if (parryData.isParryActive(currentWindow)) {

                if (!Riposte.CONFIG.allowMultiParry && parryData.getSuccessfulParryTimestamp() >= parryData.getParryTimestamp()) {
                    return;
                }

                cir.setReturnValue(false);
                player.extinguish();

                int iFrames = 10;
                var component = TrinketsApi.getTrinketComponent(player).orElse(null);

                // FALL DAMAGE PARRY EXECUTION
                if (source.isOf(DamageTypes.FALL)) {
                    if (component != null && component.isEquipped(Riposte.LEATHER_SOCK)) {
                        parryData.setLeatherSockTicks(70);
                    }

                    // NEW: Calculate the random pitch dynamically for the roll sound
                    float rollPitch = 1.0f + (player.getWorld().random.nextFloat() - 0.5f) * 0.6f;
                    player.getWorld().playSound(null, player.getBlockPos(), Riposte.PARRY_ROLL_SOUND, SoundCategory.PLAYERS, 1.0f, rollPitch);

                    parryData.setSuccessfulParryTimestamp(System.currentTimeMillis());

                    if (player.fallDistance > 8.0f && !player.getWorld().isClient) {
                        net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) player.getWorld();
                        for (int i = 0; i < 36; i++) {
                            double angle = i * (Math.PI * 2 / 36.0);
                            double px = player.getX() + Math.cos(angle) * 1.5;
                            double pz = player.getZ() + Math.sin(angle) * 1.5;
                            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.CLOUD, px, player.getY() + 0.2, pz, 1, Math.cos(angle)*0.2, 0.1, Math.sin(angle)*0.2, 0.05);
                            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.POOF, px, player.getY() + 0.2, pz, 1, Math.cos(angle)*0.2, 0.1, Math.sin(angle)*0.2, 0.05);
                        }
                    }

                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeBoolean(true);
                        ServerPlayNetworking.send(serverPlayer, Riposte.PARRY_SUCCESS_PACKET, buf);
                    }
                    return;
                }

                if (component != null) {
                    if (component.isEquipped(Riposte.COPPER_GUARD) || component.isEquipped(Riposte.VOID_GUARD)) {
                        iFrames = 30;
                    } else if (component.isEquipped(Riposte.IRON_GUARD)) {
                        iFrames = 20;
                    }
                }
                player.timeUntilRegen = iFrames;

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

                boolean isLethal = amount >= player.getHealth();
                boolean isHeavyDamage = amount >= (player.getMaxHealth() * 0.4f);

                float randomPitch = 1.0f + (player.getWorld().random.nextFloat() - 0.5f) * 0.6f;
                SoundEvent soundToPlay = isLethal ? Riposte.LETHAL_PARRY_SOUND : (isWeapon ? Riposte.WEAPON_PARRY_SOUND : Riposte.NORMAL_PARRY_SOUND);

                player.getWorld().playSound(null, player.getBlockPos(), soundToPlay, SoundCategory.PLAYERS, 1.0f, randomPitch);

                if (!player.getWorld().isClient) {
                    Vec3d look = player.getRotationVector();
                    double midX = player.getX() + (look.x * 1.2);
                    double midY = player.getEyeY() + (look.y * 1.2) - 0.2;
                    double midZ = player.getZ() + (look.z * 1.2);

                    for (ServerPlayerEntity tracker : PlayerLookup.tracking(player)) {
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeDouble(midX);
                        buf.writeDouble(midY);
                        buf.writeDouble(midZ);
                        buf.writeFloat(player.getYaw());
                        buf.writeBoolean(isWeapon);
                        buf.writeBoolean(isHeavyDamage);
                        ServerPlayNetworking.send(tracker, Riposte.PARRY_VFX_PACKET, buf);
                    }
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeDouble(midX);
                        buf.writeDouble(midY);
                        buf.writeDouble(midZ);
                        buf.writeFloat(player.getYaw());
                        buf.writeBoolean(isWeapon);
                        buf.writeBoolean(isHeavyDamage);
                        ServerPlayNetworking.send(serverPlayer, Riposte.PARRY_VFX_PACKET, buf);

                        if (isLethal) {
                            ServerPlayNetworking.send(serverPlayer, Riposte.LETHAL_VFX_PACKET, PacketByteBufs.create());
                        }
                    }
                }

                parryData.setSuccessfulParryTimestamp(System.currentTimeMillis());

                if (player instanceof ServerPlayerEntity serverPlayer) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeBoolean(false);
                    ServerPlayNetworking.send(serverPlayer, Riposte.PARRY_SUCCESS_PACKET, buf);
                }

                if (component != null && component.isEquipped(Riposte.HONORABLE_CAPE)) {
                    parryData.refundParryCooldown(Riposte.CONFIG.wanderersCapeCooldownCharge);
                }

                if (rawAttacker instanceof LivingEntity attacker) {
                    parryData.setLastParriedEntityId(attacker.getId());

                    double kbStrength = Riposte.CONFIG.parryKnockback;
                    if (component != null && component.isEquipped(Riposte.IRON_GUARD)) {
                        kbStrength *= 1.3;
                    }

                    boolean isBoss = attacker instanceof WitherEntity ||
                            attacker instanceof EnderDragonEntity ||
                            attacker instanceof WardenEntity ||
                            attacker instanceof ElderGuardianEntity;

                    if (isBoss) {
                        parryData.applyParryKnockback(attacker, kbStrength, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
                        parryData.applyParryKnockback(player, kbStrength, attacker.getX() - player.getX(), attacker.getZ() - player.getZ());
                    } else {
                        parryData.applyParryKnockback(attacker, kbStrength, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
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