package nel.riposte.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import nel.riposte.FinisherData;
import nel.riposte.HitstopData;
import nel.riposte.ParryData;
import nel.riposte.Riposte;
import nel.riposte.config.RiposteConfig;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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

    @Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
    private void riposte$cancelKnockback(double strength, double x, double z, CallbackInfo ci) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;

        if (thisEntity instanceof PlayerEntity player) {
            var component = TrinketsApi.getTrinketComponent(player).orElse(null);
            if (component != null && (component.isEquipped(Riposte.COPPER_GUARD) || component.isEquipped(Riposte.VOID_GUARD))) {
                ci.cancel();
                return;
            }
        }

        if (!thisEntity.getWorld().isClient) {
            for (PlayerEntity p : thisEntity.getWorld().getPlayers()) {
                FinisherData fData = (FinisherData) p;
                if (fData.isExecutingFinisher() && fData.getFinisherTargetId() == thisEntity.getId()) {
                    ci.cancel();
                    return;
                }
            }
        }
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float riposte$accessoryDamageModifiers(float amount, DamageSource source) {
        float modifiedAmount = amount;

        if (source.getAttacker() instanceof PlayerEntity attacker) {
            var component = TrinketsApi.getTrinketComponent(attacker).orElse(null);
            if (component != null && component.isEquipped(Riposte.HONORABLE_CAPE)) {
                modifiedAmount *= (float) Riposte.CONFIG.accessories.honorableCape.damageDealtMultiplier;
            }

            if (component != null && component.isEquipped(Riposte.BLOODLUSTFUL_RING)) {
                ParryData parryData = (ParryData) attacker;
                if (((Object) this instanceof LivingEntity victim) && victim.getId() == parryData.getLastParriedEntityId()) {
                    long timeSinceParry = System.currentTimeMillis() - parryData.getSuccessfulParryTimestamp();
                    if (timeSinceParry <= Riposte.CONFIG.kickComboWindowMs) {
                        modifiedAmount *= (float) Riposte.CONFIG.accessories.bloodlustfulRing.kickDamageMultiplier;
                    }
                }
            }
        }

        if ((Object) this instanceof PlayerEntity victim) {
            var component = TrinketsApi.getTrinketComponent(victim).orElse(null);
            if (component != null && component.isEquipped(Riposte.BLOODLUSTFUL_RING)) {
                modifiedAmount *= (float) Riposte.CONFIG.accessories.bloodlustfulRing.damageTakenMultiplier;
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
    private void riposte$cancelDamageDuringFinisher(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;

        if (thisEntity instanceof PlayerEntity player) {
            if (player instanceof FinisherData finisherData) {
                if (finisherData.isExecutingFinisher() || finisherData.getPostFinisherInvuln() > 0) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }

        if (!thisEntity.getWorld().isClient && !Riposte.CONFIG.addons.finishers.enemyDamageFinisher) {
            for (PlayerEntity p : thisEntity.getWorld().getPlayers()) {
                FinisherData fData = (FinisherData) p;
                if (fData.isExecutingFinisher() && fData.getFinisherTargetId() == thisEntity.getId()) {
                    if (source.getAttacker() != p) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void riposte$onCreeperDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;

        if (thisEntity instanceof net.minecraft.entity.mob.CreeperEntity creeper && !creeper.getWorld().isClient) {
            if (source.getSource() instanceof net.minecraft.entity.projectile.ProjectileEntity projectile) {
                if (projectile.getCommandTags().contains("riposte_from_skeleton")) {
                    if (source.getAttacker() instanceof PlayerEntity) {
                        var optional = net.minecraft.registry.Registries.ITEM.getEntryList(net.minecraft.registry.tag.ItemTags.CREEPER_DROP_MUSIC_DISCS)
                                .flatMap(entries -> entries.getRandom(creeper.getWorld().getRandom()));
                        if (optional.isPresent()) {
                            creeper.dropItem(optional.get().value());
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void riposte$onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity thisEntity = (LivingEntity) (Object) this;

        if (!thisEntity.getWorld().isClient && Riposte.CONFIG.addons.finishers.enableFinishers && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            if (source.getAttacker() instanceof PlayerEntity playerAttacker && source.getSource() != null) {
                FinisherData fData = (FinisherData) playerAttacker;
                if (fData.isProjectileDeflected(source.getSource().getId())) {
                    int victimId = thisEntity.getId();

                    if (Riposte.CONFIG.addons.finishers.finisherMode == RiposteConfig.FinisherMode.GAUGE_METER) {
                        fData.addFinisherGauge(victimId, Riposte.CONFIG.addons.finishers.finisherFillOnParry);
                    } else {
                        fData.addParryCount(victimId);
                    }

                    if (playerAttacker instanceof ServerPlayerEntity serverPlayer) {
                        PacketByteBuf syncBuf = PacketByteBufs.create();
                        syncBuf.writeInt(victimId);
                        syncBuf.writeFloat(fData.getFinisherGauge(victimId));
                        syncBuf.writeInt(fData.getParryCount(victimId));
                        ServerPlayNetworking.send(serverPlayer, Riposte.SYNC_FINISHER_GAUGE_PACKET, syncBuf);
                    }
                }
            }
        }

        if ((Object) this instanceof PlayerEntity player) {
            ParryData parryData = (ParryData) player;

            if (!parryData.canParryDamageType(source)) return;

            int currentWindow = parryData.getCalculatedWindow(Riposte.CONFIG.parryWindowMs);

            if (source.isOf(DamageTypes.FALL) && !parryData.isParryActive(currentWindow)) {
                var component = TrinketsApi.getTrinketComponent(player).orElse(null);
                int neuralCount = component != null ? component.getEquipped(Riposte.NEURAL_LINK).size() : 0;

                if (neuralCount > 0 && player.getWorld().getRandom().nextFloat() < (Riposte.CONFIG.accessories.neuralLink.autoParryChance * neuralCount)) {
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

                if (source.isOf(DamageTypes.FALL)) {
                    if (component != null && component.isEquipped(Riposte.LEATHER_SOCK)) {
                        parryData.setLeatherSockTicks(Riposte.CONFIG.accessories.leatherSocks.speedDurationTicks);
                    }

                    float rollPitch = 1.0f + (player.getWorld().random.nextFloat() - 0.5f) * 0.6f;
                    player.getWorld().playSound(null, player.getBlockPos(), Riposte.PARRY_ROLL_SOUND, SoundCategory.PLAYERS, 1.0f, rollPitch);

                    parryData.setSuccessfulParryTimestamp(System.currentTimeMillis());

                    if (!player.getWorld().isClient) {
                        for (ServerPlayerEntity tracker : PlayerLookup.tracking(player)) {
                            PacketByteBuf vfxBuf = PacketByteBufs.create();
                            vfxBuf.writeDouble(player.getX());
                            vfxBuf.writeDouble(player.getY());
                            vfxBuf.writeDouble(player.getZ());
                            ServerPlayNetworking.send(tracker, Riposte.FALL_PARRY_VFX_PACKET, vfxBuf);
                        }
                        if (player instanceof ServerPlayerEntity serverPlayer) {
                            PacketByteBuf vfxBuf = PacketByteBufs.create();
                            vfxBuf.writeDouble(player.getX());
                            vfxBuf.writeDouble(player.getY());
                            vfxBuf.writeDouble(player.getZ());
                            ServerPlayNetworking.send(serverPlayer, Riposte.FALL_PARRY_VFX_PACKET, vfxBuf);
                        }
                    }

                    if (Riposte.CONFIG.enableSuccessParryRecharge) {
                        parryData.refundParryCooldown((float) Riposte.CONFIG.globalParryCooldownRecharge);
                    }
                    if (component != null && component.isEquipped(Riposte.HONORABLE_CAPE)) {
                        parryData.refundParryCooldown((float) Riposte.CONFIG.accessories.honorableCape.cooldownCharge);
                    }

                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeBoolean(true);
                        ServerPlayNetworking.send(serverPlayer, Riposte.PARRY_SUCCESS_PACKET, buf);
                    }
                    return;
                }

                if (component != null) {
                    if (component.isEquipped(Riposte.COPPER_GUARD)) iFrames = Riposte.CONFIG.accessories.copperGuard.invulnTimeTicks;
                    else if (component.isEquipped(Riposte.VOID_GUARD)) iFrames = Riposte.CONFIG.accessories.voidGuard.invulnTimeTicks;
                    else if (component.isEquipped(Riposte.IRON_GUARD)) iFrames = Riposte.CONFIG.accessories.ironGuard.invulnTimeTicks;
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

                if (!player.getWorld().isClient && Riposte.CONFIG.addons.finishers.enableFinishers) {
                    if (Riposte.CONFIG.addons.finishers.finisherFillOn == RiposteConfig.FinisherTrigger.NORMAL_PARRY || Riposte.CONFIG.addons.finishers.finisherFillOn == RiposteConfig.FinisherTrigger.BOTH) {
                        FinisherData finisherData = (FinisherData) player;
                        if (rawAttacker != null) {
                            int victimId = rawAttacker.getId();
                            if (Riposte.CONFIG.addons.finishers.finisherMode == RiposteConfig.FinisherMode.GAUGE_METER) {
                                finisherData.addFinisherGauge(victimId, Riposte.CONFIG.addons.finishers.finisherFillOnParry);
                            } else {
                                finisherData.addParryCount(victimId);
                            }

                            if (player instanceof ServerPlayerEntity serverPlayer) {
                                PacketByteBuf syncBuf = PacketByteBufs.create();
                                syncBuf.writeInt(victimId);
                                syncBuf.writeFloat(finisherData.getFinisherGauge(victimId));
                                syncBuf.writeInt(finisherData.getParryCount(victimId));
                                ServerPlayNetworking.send(serverPlayer, Riposte.SYNC_FINISHER_GAUGE_PACKET, syncBuf);
                            }
                        }
                    }
                }

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

                if (Riposte.CONFIG.enableSuccessParryRecharge) {
                    parryData.refundParryCooldown((float) Riposte.CONFIG.globalParryCooldownRecharge);
                }
                if (component != null && component.isEquipped(Riposte.HONORABLE_CAPE)) {
                    parryData.refundParryCooldown((float) Riposte.CONFIG.accessories.honorableCape.cooldownCharge);
                }

                if (rawAttacker instanceof LivingEntity attacker) {
                    parryData.setLastParriedEntityId(attacker.getId());

                    double kbStrength = Riposte.CONFIG.parryKnockback;
                    if (component != null && component.isEquipped(Riposte.IRON_GUARD)) {
                        kbStrength *= Riposte.CONFIG.accessories.ironGuard.knockbackMultiplier;
                    }

                    boolean isBoss = attacker instanceof WitherEntity ||
                            attacker instanceof EnderDragonEntity ||
                            attacker instanceof WardenEntity ||
                            attacker instanceof ElderGuardianEntity;

                    if (isBoss) {
                        parryData.applyParryKnockback(attacker, kbStrength, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
                        Vec3d atkVel = attacker.getVelocity();
                        attacker.setVelocity(atkVel.x, Math.max(atkVel.y, 0.3), atkVel.z);
                        attacker.velocityModified = true;

                        parryData.applyParryKnockback(player, kbStrength, attacker.getX() - player.getX(), attacker.getZ() - player.getZ());
                        Vec3d pVel = player.getVelocity();
                        player.setVelocity(pVel.x, Math.max(pVel.y, 0.3), pVel.z);
                        player.velocityModified = true;
                    } else {
                        parryData.applyParryKnockback(attacker, kbStrength, player.getX() - attacker.getX(), player.getZ() - attacker.getZ());
                        Vec3d atkVel = attacker.getVelocity();
                        attacker.setVelocity(atkVel.x, Math.max(atkVel.y, 0.3), atkVel.z);
                        attacker.velocityModified = true;
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