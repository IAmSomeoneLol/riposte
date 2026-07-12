package nel.riposte.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import nel.riposte.FinisherData;
import nel.riposte.FinisherDefinition;
import nel.riposte.FinisherLoader;
import nel.riposte.ParryData;
import nel.riposte.Riposte;
import nel.riposte.config.RiposteConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements ParryData, FinisherData {

    @Unique private long parryTimestamp = 0L;
    @Unique private long successfulParryTimestamp = 0L;
    @Unique private long successfulComboTimestamp = 0L;
    @Unique private int lastParriedEntityId = -1;

    @Unique private int leatherSockTicks = 0;
    @Unique private static final UUID LEATHER_SOCK_SPEED_UUID = UUID.fromString("b5b8d810-761e-45fa-8eb8-4dc4eb6b871c");

    @Unique private int riposte$shulkerAttackSpeedTicks = 0;
    @Unique private static final UUID SHULKER_ATTACK_SPEED_UUID = UUID.fromString("e5c3c0a1-7b0b-4b2a-8c1a-2d3b4a5b6c7d");

    @Unique private boolean riposte$isExecutingFinisher = false;
    @Unique private int riposte$finisherTargetId = -1;
    @Unique private int riposte$finisherTick = 0;

    @Unique private String riposte$activeFinisherId = "";

    @Unique private int riposte$postFinisherInvulnTicks = 0;

    @Unique private Map<Integer, Float> riposte$gaugeMeters = new HashMap<>();
    @Unique private Map<Integer, Integer> riposte$parryCounts = new HashMap<>();
    @Unique private Map<Integer, Long> riposte$gaugeReadyTimestamps = new HashMap<>();
    @Unique private Map<Integer, Long> riposte$parryReadyTimestamps = new HashMap<>();
    @Unique private Set<Integer> riposte$deflectedProjectiles = new HashSet<>();

    @Override public long getParryTimestamp() { return this.parryTimestamp; }
    @Override public void setParryTimestamp(long timestamp) { this.parryTimestamp = timestamp; }
    @Override public long getSuccessfulParryTimestamp() { return this.successfulParryTimestamp; }
    @Override public void setSuccessfulParryTimestamp(long timestamp) { this.successfulParryTimestamp = timestamp; }
    @Override public long getSuccessfulComboTimestamp() { return this.successfulComboTimestamp; }
    @Override public void setSuccessfulComboTimestamp(long timestamp) { this.successfulComboTimestamp = timestamp; }
    @Override public int getLastParriedEntityId() { return this.lastParriedEntityId; }
    @Override public void setLastParriedEntityId(int id) { this.lastParriedEntityId = id; }
    @Override public void setLeatherSockTicks(int ticks) { this.leatherSockTicks = ticks; }
    @Override public int getLeatherSockTicks() { return this.leatherSockTicks; }

    @Override public boolean isExecutingFinisher() { return this.riposte$isExecutingFinisher; }
    @Override public int getFinisherTargetId() { return this.riposte$finisherTargetId; }
    @Override public int getFinisherTick() { return this.riposte$finisherTick; }

    @Override public String getActiveFinisherId() { return this.riposte$activeFinisherId; }
    @Override public void setActiveFinisherId(String id) { this.riposte$activeFinisherId = id; }

    @Override public int getPostFinisherInvuln() { return this.riposte$postFinisherInvulnTicks; }
    @Override public void setPostFinisherInvuln(int ticks) { this.riposte$postFinisherInvulnTicks = ticks; }

    @Override public void markProjectileDeflected(int projectileId) { this.riposte$deflectedProjectiles.add(projectileId); }
    @Override public boolean isProjectileDeflected(int projectileId) { return this.riposte$deflectedProjectiles.contains(projectileId); }

    @Override public float getFinisherGauge(int targetId) { return this.riposte$gaugeMeters.getOrDefault(targetId, 0f); }
    @Override public int getParryCount(int targetId) { return this.riposte$parryCounts.getOrDefault(targetId, 0); }
    @Override public void clearParryCount(int targetId) { this.riposte$parryCounts.remove(targetId); this.riposte$parryReadyTimestamps.remove(targetId); }
    @Override public long getGaugeReadyTimestamp(int targetId) { return this.riposte$gaugeReadyTimestamps.getOrDefault(targetId, 0L); }
    @Override public void setGaugeReadyTimestamp(int targetId, long time) { this.riposte$gaugeReadyTimestamps.put(targetId, time); }
    @Override public long getParryReadyTimestamp(int targetId) { return this.riposte$parryReadyTimestamps.getOrDefault(targetId, 0L); }
    @Override public void setParryReadyTimestamp(int targetId, long time) { this.riposte$parryReadyTimestamps.put(targetId, time); }

    @Override
    public void addFinisherGauge(int targetId, float amount) {
        float before = getFinisherGauge(targetId);
        float after = Math.min(300f, before + amount);
        this.riposte$gaugeMeters.put(targetId, after);
    }

    @Override
    public void consumeFinisherGauge(int targetId, float amount) {
        float after = Math.max(0f, getFinisherGauge(targetId) - amount);
        this.riposte$gaugeMeters.put(targetId, after);
        if (after < 100f) {
            this.riposte$gaugeReadyTimestamps.remove(targetId);
        }
    }

    @Override
    public void setFinisherGauge(int targetId, float amount) {
        this.riposte$gaugeMeters.put(targetId, amount);
        if (amount < 100f) {
            this.riposte$gaugeReadyTimestamps.remove(targetId);
        }
    }

    @Override
    public void addParryCount(int targetId) {
        int current = getParryCount(targetId);
        this.riposte$parryCounts.put(targetId, current + 1);
    }

    @Override
    public void setParryCount(int targetId, int amount) {
        this.riposte$parryCounts.put(targetId, amount);
        if (amount < Riposte.CONFIG.addons.finishers.finisherParryCountMax) {
            this.riposte$parryReadyTimestamps.remove(targetId);
        }
    }

    @Override
    public void startFinisher(int targetId, String finisherId) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        this.riposte$isExecutingFinisher = true;
        this.riposte$finisherTargetId = targetId;
        this.riposte$finisherTick = 0;
        this.riposte$activeFinisherId = finisherId;

        if (!player.getWorld().isClient) {
            for (ServerPlayerEntity tracker : PlayerLookup.tracking(player)) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeUuid(player.getUuid());
                buf.writeInt(targetId);
                buf.writeString(finisherId);
                ServerPlayNetworking.send(tracker, Riposte.START_FINISHER_ANIM_PACKET, buf);
            }
            if (player instanceof ServerPlayerEntity serverPlayer) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeUuid(player.getUuid());
                buf.writeInt(targetId);
                buf.writeString(finisherId);
                ServerPlayNetworking.send(serverPlayer, Riposte.START_FINISHER_ANIM_PACKET, buf);
            }
        }
    }

    @Override
    public void cancelFinisher() {
        if (this.riposte$isExecutingFinisher) {
            this.riposte$postFinisherInvulnTicks = 8;
        }
        this.riposte$isExecutingFinisher = false;
        this.riposte$finisherTick = 0;
        this.riposte$activeFinisherId = "";
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void riposte$tickPostFinisherInvuln(CallbackInfo ci) {
        if (this.riposte$postFinisherInvulnTicks > 0) {
            this.riposte$postFinisherInvulnTicks--;
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void riposte$lockPlayerDuringFinisher(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (this.riposte$isExecutingFinisher) {
            player.forwardSpeed = 0.0f;
            player.sidewaysSpeed = 0.0f;
            player.upwardSpeed = 0.0f;
            player.setJumping(false);
            player.setSprinting(false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void riposte$processFinisherSequence(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        long now = System.currentTimeMillis();

        if (Riposte.CONFIG.addons.finishers.enableFinishers && player.age % 20 == 0) {
            if (Riposte.CONFIG.addons.finishers.finisherMode == RiposteConfig.FinisherMode.GAUGE_METER) {
                this.riposte$gaugeMeters.keySet().removeIf(id -> {
                    net.minecraft.entity.Entity targetRaw = player.getWorld().getEntityById(id);
                    if (!(targetRaw instanceof LivingEntity target) || target.isRemoved()) {
                        this.riposte$gaugeReadyTimestamps.remove(id);
                        return true;
                    }

                    float current = this.riposte$gaugeMeters.getOrDefault(id, 0f);
                    if (current >= 100f) {
                        float healthPercent = target.getMaxHealth() > 0 ? (target.getHealth() / target.getMaxHealth()) * 100f : 0f;
                        boolean healthEligible = Riposte.CONFIG.addons.finishers.isHealthEligible(healthPercent);
                        long readyTime = this.getGaugeReadyTimestamp(id);

                        if (healthEligible) {
                            if (readyTime <= 0) {
                                this.setGaugeReadyTimestamp(id, now);
                            } else if (now - readyTime > Riposte.CONFIG.addons.finishers.finisherTimeoutMs) {
                                this.riposte$gaugeReadyTimestamps.remove(id);
                                return true;
                            }
                        } else if (readyTime > 0) {
                            this.riposte$gaugeReadyTimestamps.remove(id);
                        }
                    } else if (current > 0) {
                        this.riposte$gaugeMeters.put(id, Math.max(0f, current - Riposte.CONFIG.addons.finishers.finisherGaugeConsumptionPerSecond));
                    }
                    return false;
                });
            } else {
                this.riposte$parryCounts.entrySet().removeIf(entry -> {
                    int targetId = entry.getKey();
                    net.minecraft.entity.Entity targetRaw = player.getWorld().getEntityById(targetId);
                    if (!(targetRaw instanceof LivingEntity target) || target.isRemoved()) {
                        this.riposte$parryReadyTimestamps.remove(targetId);
                        return true;
                    }

                    if (entry.getValue() >= Riposte.CONFIG.addons.finishers.finisherParryCountMax) {
                        float healthPercent = target.getMaxHealth() > 0 ? (target.getHealth() / target.getMaxHealth()) * 100f : 0f;
                        boolean healthEligible = Riposte.CONFIG.addons.finishers.isHealthEligible(healthPercent);
                        long readyTime = this.getParryReadyTimestamp(targetId);

                        if (healthEligible) {
                            if (readyTime <= 0) {
                                this.setParryReadyTimestamp(targetId, now);
                                return false;
                            }
                            return now - readyTime > Riposte.CONFIG.addons.finishers.finisherTimeoutMs;
                        } else if (readyTime > 0) {
                            this.riposte$parryReadyTimestamps.remove(targetId);
                        }
                    }
                    return false;
                });
            }
        }

        if (this.riposte$isExecutingFinisher) {
            this.riposte$finisherTick++;

            FinisherDefinition def = FinisherLoader.getFinisherById(this.riposte$activeFinisherId);
            if (def == null) {
                this.cancelFinisher();
                return;
            }

            Entity targetRaw = player.getWorld().getEntityById(this.riposte$finisherTargetId);

            if (targetRaw instanceof LivingEntity target && !target.isRemoved()) {

                Vec3d diff = target.getPos().subtract(player.getPos()).multiply(1.0, 0, 1.0);
                if (diff.lengthSquared() < 0.01) diff = player.getRotationVector().multiply(1.0, 0, 1.0);
                Vec3d toTarget = diff.normalize();

                double requiredDistance = (target.getWidth() / 2.0) + 0.5;
                double idealX = target.getX() - toTarget.x * requiredDistance;
                double idealZ = target.getZ() - toTarget.z * requiredDistance;

                double dx = idealX - player.getX();
                double dz = idealZ - player.getZ();

                if (diff.length() > requiredDistance) {
                    player.setVelocity(dx * 0.4, player.getVelocity().y, dz * 0.4);
                } else {
                    player.setVelocity(0, player.getVelocity().y, 0);
                }

                if (!player.getWorld().isClient) {

                    double rotDx = player.getX() - target.getX();
                    double rotDz = player.getZ() - target.getZ();
                    float targetYaw = (float) Math.toDegrees(Math.atan2(rotDz, rotDx)) - 90.0f;

                    target.setYaw(targetYaw);
                    target.setHeadYaw(targetYaw);
                    target.setBodyYaw(targetYaw);

                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 255, false, false, false));
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 10, 255, false, false, false));

                    if (def.timeline != null) {
                        for (FinisherDefinition.TimelineEvent event : def.timeline) {
                            if (event.tick == this.riposte$finisherTick) {

                                if ("sound".equals(event.action)) {
                                    String targetSound = null;
                                    if (event.values != null && !event.values.isEmpty()) {
                                        targetSound = event.values.get(player.getWorld().random.nextInt(event.values.size()));
                                    } else if (event.value != null) {
                                        targetSound = event.value;
                                    }

                                    if (targetSound != null) {
                                        SoundEvent sound = Registries.SOUND_EVENT.get(new Identifier(targetSound));
                                        if (sound != null) {
                                            float randomPitch = 1.0f + (player.getWorld().random.nextFloat() - 0.5f) * 0.4f;
                                            player.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(), sound, SoundCategory.PLAYERS, 1.0f, randomPitch);
                                        }
                                    }
                                }

                                else if ("damage".equals(event.action)) {
                                    float dmg = 0f;
                                    boolean isLethal = false;

                                    if (event.value != null && event.value.equals("lethal")) {
                                        dmg = Float.MAX_VALUE;
                                        isLethal = true;
                                    } else if (event.value != null && event.value.equals("half_current")) {
                                        dmg = Math.max(1.0f, target.getHealth() * 0.5f);
                                    } else {
                                        dmg = event.amount;
                                    }

                                    target.damage(player.getDamageSources().playerAttack(player), dmg);

                                    if (isLethal) {
                                        target.setHealth(0);

                                        if (Riposte.CONFIG.addons.finishers.finisherRewardEnabled) {
                                            float hpPercent = Riposte.CONFIG.addons.finishers.finisherHealthReturnPercent / 100.0f;
                                            player.heal(player.getMaxHealth() * hpPercent);

                                            float foodPercent = Riposte.CONFIG.addons.finishers.finisherFoodReturnPercent / 100.0f;
                                            int foodReward = Math.round(20.0f * foodPercent);
                                            float satReward = 20.0f * foodPercent;

                                            net.minecraft.entity.player.HungerManager hunger = player.getHungerManager();
                                            if (hunger.getFoodLevel() < 20) {
                                                hunger.setFoodLevel(Math.min(20, hunger.getFoodLevel() + foodReward));
                                            } else {
                                                hunger.setSaturationLevel(Math.min(20.0f, hunger.getSaturationLevel() + satReward));
                                            }
                                        }
                                    }
                                }

                                else if ("entity_pitch".equals(event.action)) {
                                    target.setPitch(event.amount);
                                    target.setHeadYaw(target.getYaw());
                                }

                                else if ("blood".equals(event.action)) {
                                    if (player.getWorld() instanceof ServerWorld sw) {
                                        sw.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState()),
                                                target.getX() + event.x_offset,
                                                target.getY() + target.getHeight() * 0.5 + event.y_offset,
                                                target.getZ() + event.z_offset,
                                                event.count == 0 ? 25 : event.count,
                                                0.2, 0.2, 0.2, 0.15);
                                    }
                                }

                                else if ("particle".equals(event.action) && event.value != null) {
                                    DefaultParticleType pType = (DefaultParticleType) Registries.PARTICLE_TYPE.get(new Identifier(event.value));
                                    if (pType != null && player.getWorld() instanceof ServerWorld sw) {
                                        sw.spawnParticles(pType, target.getX() + event.x_offset, target.getY() + target.getHeight() * 0.5 + event.y_offset, target.getZ() + event.z_offset, event.count == 0 ? 10 : event.count, 0.2, 0.2, 0.2, 0.15);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (!player.getWorld().isClient) this.cancelFinisher();
            }

            if (this.riposte$finisherTick >= def.total_duration_ticks) {
                this.cancelFinisher();
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void riposte$tickAccessoryAttributes(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!player.getWorld().isClient) {
            var speedAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                if (this.leatherSockTicks > 0) {
                    this.leatherSockTicks--;
                    if (speedAttr.getModifier(LEATHER_SOCK_SPEED_UUID) == null) {
                        speedAttr.addTemporaryModifier(new EntityAttributeModifier(LEATHER_SOCK_SPEED_UUID, "Leather Sock Speed", Riposte.CONFIG.accessories.leatherSocks.speedMultiplier, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
                    }
                } else {
                    if (speedAttr.getModifier(LEATHER_SOCK_SPEED_UUID) != null) {
                        speedAttr.removeModifier(LEATHER_SOCK_SPEED_UUID);
                    }
                }
            }

            var attackSpeedAttr = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
            if (attackSpeedAttr != null) {
                if (this.riposte$shulkerAttackSpeedTicks > 0) {
                    this.riposte$shulkerAttackSpeedTicks--;
                    if (attackSpeedAttr.getModifier(SHULKER_ATTACK_SPEED_UUID) == null) {
                        attackSpeedAttr.addTemporaryModifier(new EntityAttributeModifier(SHULKER_ATTACK_SPEED_UUID, "Shulker Attack Speed", Riposte.CONFIG.accessories.shulkerHeadPlate.attackSpeedBoost, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
                    }
                } else {
                    if (attackSpeedAttr.getModifier(SHULKER_ATTACK_SPEED_UUID) != null) {
                        attackSpeedAttr.removeModifier(SHULKER_ATTACK_SPEED_UUID);
                    }
                }
            }
        }
    }

    @Inject(method = "setFireTicks", at = @At("HEAD"), cancellable = true)
    private void riposte$blockFireDuringParry(int ticks, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        int currentWindow = this.getCalculatedWindow(Riposte.CONFIG.parryWindowMs);

        if (ticks > player.getFireTicks() && this.isParryActive(currentWindow)) {
            ci.cancel();
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void riposte$blockAttackDuringFinisher(Entity target, CallbackInfo ci) {
        if (this.riposte$isExecutingFinisher) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void riposte$airParryProjectiles(CallbackInfo ci) {
        if (!Riposte.CONFIG.deflectProjectiles) return;

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getWorld().isClient) return;

        if (!this.canParryProjectiles()) return;

        int currentWindow = this.getCalculatedWindow(Riposte.CONFIG.parryWindowMs);
        if (this.isParryActive(currentWindow)) {

            if (!Riposte.CONFIG.allowMultiParry && this.successfulParryTimestamp >= this.parryTimestamp) {
                return;
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

                Entity originalOwner = projectile.getOwner();

                // NATIVE TRACKING: Appends vanilla command tags (scoreboard tags) safely
                if (originalOwner instanceof net.minecraft.entity.mob.AbstractSkeletonEntity) {
                    projectile.addCommandTag("riposte_from_skeleton");
                }

                projectile.setOwner(player);

                projectile.setPosition(
                        player.getX() + (lookDir.x * 0.5),
                        player.getEyeY() + (lookDir.y * 0.5),
                        player.getZ() + (lookDir.z * 0.5)
                );

                if (projectile instanceof PersistentProjectileEntity arrow) {
                    ((nel.riposte.mixin.PersistentProjectileEntityAccessor) arrow).setInGround(false);
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

                boolean isHeavyDamage = speed > 2.0;

                float randomPitch = 1.0f + (player.getWorld().random.nextFloat() - 0.5f) * 0.6f;
                SoundEvent soundToPlay = isWeapon ? Riposte.WEAPON_PARRY_SOUND : Riposte.NORMAL_PARRY_SOUND;

                player.getWorld().playSound(null, player.getBlockPos(), soundToPlay, SoundCategory.PLAYERS, 1.0f, randomPitch);

                if (!player.getWorld().isClient && Riposte.CONFIG.addons.finishers.enableFinishers) {
                    if (Riposte.CONFIG.addons.finishers.finisherFillOn == RiposteConfig.FinisherTrigger.NORMAL_PARRY || Riposte.CONFIG.addons.finishers.finisherFillOn == RiposteConfig.FinisherTrigger.BOTH) {
                        FinisherData finisherData = (FinisherData) player;
                        if (Riposte.CONFIG.addons.finishers.finisherMode == RiposteConfig.FinisherMode.GAUGE_METER) {
                            finisherData.addFinisherGauge(originalOwner != null ? originalOwner.getId() : -1, Riposte.CONFIG.addons.finishers.finisherFillOnParry);
                        } else if (originalOwner != null) {
                            finisherData.addParryCount(originalOwner.getId());
                        }
                    }
                }

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
                    }
                }

                this.setSuccessfulParryTimestamp(System.currentTimeMillis());
                this.setLastParriedEntityId(projectile.getId());

                if (Riposte.CONFIG.enableSuccessParryRecharge) {
                    this.refundParryCooldown((float) Riposte.CONFIG.globalParryCooldownRecharge);
                }
                var component = TrinketsApi.getTrinketComponent(player).orElse(null);
                if (component != null && component.isEquipped(Riposte.HONORABLE_CAPE)) {
                    this.refundParryCooldown((float) Riposte.CONFIG.accessories.honorableCape.cooldownCharge);
                }

                if (player instanceof ServerPlayerEntity serverPlayer) {
                    PacketByteBuf successBuf = PacketByteBufs.create();
                    successBuf.writeBoolean(false);
                    successBuf.writeInt(originalOwner != null ? originalOwner.getId() : -1);
                    successBuf.writeFloat(((FinisherData) player).getFinisherGauge(originalOwner != null ? originalOwner.getId() : -1));
                    ServerPlayNetworking.send(serverPlayer, Riposte.PARRY_SUCCESS_PACKET, successBuf);
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
                    int victimId = livingTarget.getId();

                    if (!player.getWorld().isClient && Riposte.CONFIG.addons.finishers.enableFinishers) {
                        if (Riposte.CONFIG.addons.finishers.finisherFillOn == RiposteConfig.FinisherTrigger.KICK_COMBO || Riposte.CONFIG.addons.finishers.finisherFillOn == RiposteConfig.FinisherTrigger.BOTH) {
                            FinisherData fData = (FinisherData) player;
                            if (Riposte.CONFIG.addons.finishers.finisherMode == RiposteConfig.FinisherMode.GAUGE_METER) {
                                fData.addFinisherGauge(victimId, Riposte.CONFIG.addons.finishers.finisherFillOnKickCombo);
                            } else {
                                fData.addParryCount(victimId);
                            }

                            if (player instanceof ServerPlayerEntity serverPlayer) {
                                PacketByteBuf syncBuf = PacketByteBufs.create();
                                syncBuf.writeInt(victimId);
                                syncBuf.writeFloat(fData.getFinisherGauge(victimId));
                                syncBuf.writeInt(fData.getParryCount(victimId));
                                ServerPlayNetworking.send(serverPlayer, Riposte.SYNC_FINISHER_GAUGE_PACKET, syncBuf);
                            }
                        }
                    }

                    double heavyKnockback = Riposte.CONFIG.parryKnockback * Riposte.CONFIG.kickComboKnockbackMultiplier;

                    var component = TrinketsApi.getTrinketComponent(player).orElse(null);
                    if (component != null && component.isEquipped(Riposte.IRON_GUARD)) {
                        heavyKnockback *= Riposte.CONFIG.accessories.ironGuard.knockbackMultiplier;
                    }
                    if (component != null && component.isEquipped(Riposte.SHULKER_HEAD_PLATE)) {
                        this.riposte$shulkerAttackSpeedTicks = Riposte.CONFIG.accessories.shulkerHeadPlate.attackSpeedDurationTicks;
                    }

                    this.applyParryKnockback(livingTarget, heavyKnockback, player.getX() - livingTarget.getX(), player.getZ() - livingTarget.getZ());
                    Vec3d targetVel = livingTarget.getVelocity();
                    livingTarget.setVelocity(targetVel.x, Math.max(targetVel.y, 0.3), targetVel.z);
                    livingTarget.velocityModified = true;

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