package nel.riposte;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import nel.riposte.config.RiposteConfig;
import nel.riposte.item.RiposteAccessoryItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.Registries;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Riposte implements ModInitializer {
	public static final String MOD_ID = "riposte";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static RiposteConfig CONFIG;

	public static final DefaultParticleType PARRY_TRAIL = FabricParticleTypes.simple();
	public static final DefaultParticleType PARRY_TRAIL_LIGHT = FabricParticleTypes.simple();

	public static final Identifier WEAPON_PARRY_ID = new Identifier(MOD_ID, "weapon_parry");
	public static final SoundEvent WEAPON_PARRY_SOUND = SoundEvent.of(WEAPON_PARRY_ID);

	public static final Identifier NORMAL_PARRY_ID = new Identifier(MOD_ID, "normal_parry");
	public static final SoundEvent NORMAL_PARRY_SOUND = SoundEvent.of(NORMAL_PARRY_ID);

	public static final Identifier KICK_COMBO_ID = new Identifier(MOD_ID, "kick_combo");
	public static final SoundEvent KICK_COMBO_SOUND = SoundEvent.of(KICK_COMBO_ID);

	public static final Identifier LETHAL_PARRY_ID = new Identifier(MOD_ID, "lethal_parry");
	public static final SoundEvent LETHAL_PARRY_SOUND = SoundEvent.of(LETHAL_PARRY_ID);

	public static final Identifier PARRY_FIST_READY_ID = new Identifier(MOD_ID, "parry_fist_ready");
	public static final SoundEvent PARRY_FIST_READY_SOUND = SoundEvent.of(PARRY_FIST_READY_ID);

	public static final Identifier PARRY_WEAPON_READY_ID = new Identifier(MOD_ID, "parry_weapon_ready");
	public static final SoundEvent PARRY_WEAPON_READY_SOUND = SoundEvent.of(PARRY_WEAPON_READY_ID);

	public static final Identifier PARRY_ROLL_ID = new Identifier(MOD_ID, "parry_roll");
	public static final SoundEvent PARRY_ROLL_SOUND = SoundEvent.of(PARRY_ROLL_ID);

	public static final Identifier FINISHER_FIST1_ID = new Identifier(MOD_ID, "finisher_fist1");
	public static final SoundEvent FINISHER_FIST1_SOUND = SoundEvent.of(FINISHER_FIST1_ID);

	public static final Identifier FINISHER_FIST2_ID = new Identifier(MOD_ID, "finisher_fist2");
	public static final SoundEvent FINISHER_FIST2_SOUND = SoundEvent.of(FINISHER_FIST2_ID);

	public static final Identifier FINISHER_FIST3_ID = new Identifier(MOD_ID, "finisher_fist3");
	public static final SoundEvent FINISHER_FIST3_SOUND = SoundEvent.of(FINISHER_FIST3_ID);

	public static final Item IRON_GUARD = new RiposteAccessoryItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE), "parry", null) {
		@Override
		public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
			super.appendTooltip(stack, world, tooltip, context);
			tooltip.add(Text.translatable("tooltip.riposte.passive.projectile").formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.modifier.recharge_rate", Riposte.CONFIG.accessories.ironGuard.rechargeRate).formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.modifier.knockback", Riposte.CONFIG.accessories.ironGuard.knockbackMultiplier).formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.modifier.invuln_time", Riposte.CONFIG.accessories.ironGuard.invulnTimeTicks / 20.0f).formatted(Formatting.BLUE));
		}
	};

	public static final Item LEATHER_SOCK = new RiposteAccessoryItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE), "parry", null) {
		@Override
		public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
			super.appendTooltip(stack, world, tooltip, context);
			tooltip.add(Text.translatable("tooltip.riposte.passive.fall").formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.modifier.speed_on_fall_parry",
					(int)(Riposte.CONFIG.accessories.leatherSocks.speedMultiplier * 100),
					Riposte.CONFIG.accessories.leatherSocks.speedDurationTicks / 20.0f).formatted(Formatting.BLUE));
		}
	};

	public static final Item VOID_GUARD = new RiposteAccessoryItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC), "parry", null) {
		@Override
		public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
			super.appendTooltip(stack, world, tooltip, context);
			tooltip.add(Text.translatable("tooltip.riposte.passive.all").formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.passive.infinite_kb").formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.modifier.recharge_rate", Riposte.CONFIG.accessories.voidGuard.rechargeRate).formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.modifier.invuln_time", Riposte.CONFIG.accessories.voidGuard.invulnTimeTicks / 20.0f).formatted(Formatting.BLUE));
		}
	};

	public static final Item COPPER_GUARD = new RiposteAccessoryItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC), "parry", null) {
		@Override
		public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
			super.appendTooltip(stack, world, tooltip, context);
			tooltip.add(Text.translatable("tooltip.riposte.passive.projectile").formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.passive.fall").formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.passive.infinite_kb").formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.modifier.recharge_rate", Riposte.CONFIG.accessories.copperGuard.rechargeRate).formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.modifier.invuln_time", Riposte.CONFIG.accessories.copperGuard.invulnTimeTicks / 20.0f).formatted(Formatting.BLUE));
		}
	};

	public static final Item BLOODLUSTFUL_RING = new RiposteAccessoryItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE), "ring", null) {
		@Override
		public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
			super.appendTooltip(stack, world, tooltip, context);
			tooltip.add(Text.translatable("tooltip.riposte.passive.bloodring_1", Riposte.CONFIG.accessories.bloodlustfulRing.windowMultiplier).formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.modifier.kick_damage", Riposte.CONFIG.accessories.bloodlustfulRing.kickDamageMultiplier).formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.debuff.damage_taken", Riposte.CONFIG.accessories.bloodlustfulRing.damageTakenMultiplier).formatted(Formatting.DARK_RED));
			tooltip.add(Text.translatable("tooltip.riposte.warning.no_stack").formatted(Formatting.DARK_RED));
		}
	};

	public static final Item HONORABLE_CAPE = new RiposteAccessoryItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE), "cape", null) {
		@Override
		public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
			super.appendTooltip(stack, world, tooltip, context);
			tooltip.add(Text.translatable("tooltip.riposte.passive.charge_meter").formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.passive.damage_dealt", Riposte.CONFIG.accessories.honorableCape.damageDealtMultiplier).formatted(Formatting.BLUE));
		}
	};

	public static final Item NEURAL_LINK = new RiposteAccessoryItem(new Item.Settings().maxCount(1).rarity(Rarity.RARE), "hat", null) {
		@Override
		public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
			super.appendTooltip(stack, world, tooltip, context);
			tooltip.add(Text.translatable("tooltip.riposte.modifier.recharge_rate_percent", (int)((Riposte.CONFIG.accessories.neuralLink.rechargeMultiplier - 1.0) * 100)).formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.modifier.auto_fall_parry", (int)(Riposte.CONFIG.accessories.neuralLink.autoParryChance * 100)).formatted(Formatting.BLUE));
		}
	};

	public static final Item SHULKER_HEAD_PLATE = new RiposteAccessoryItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC), "charm", "head/hat") {
		@Override
		protected int getBonusSlotCount() {
			return Riposte.CONFIG.accessories.shulkerHeadPlate.slotBonus;
		}

		@Override
		public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
			super.appendTooltip(stack, world, tooltip, context);
			tooltip.add(Text.translatable("tooltip.riposte.passive.head_slot", Riposte.CONFIG.accessories.shulkerHeadPlate.slotBonus).formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.modifier.attack_speed_on_kick_combo",
					(int)(Riposte.CONFIG.accessories.shulkerHeadPlate.attackSpeedBoost * 100),
					Riposte.CONFIG.accessories.shulkerHeadPlate.attackSpeedDurationTicks / 20.0f).formatted(Formatting.BLUE));
			tooltip.add(Text.translatable("tooltip.riposte.warning.no_stack").formatted(Formatting.DARK_RED));
		}
	};

	public static final Identifier PARRY_SYNC_PACKET = new Identifier(MOD_ID, "parry_sync");
	public static final Identifier PARRY_SUCCESS_PACKET = new Identifier(MOD_ID, "parry_success");
	public static final Identifier COMBO_SUCCESS_PACKET = new Identifier(MOD_ID, "combo_success");
	public static final Identifier PARRY_VFX_PACKET = new Identifier(MOD_ID, "parry_vfx");
	public static final Identifier FALL_PARRY_VFX_PACKET = new Identifier(MOD_ID, "fall_parry_vfx");
	public static final Identifier LETHAL_VFX_PACKET = new Identifier(MOD_ID, "lethal_vfx");

	public static final Identifier EXECUTE_FINISHER_PACKET = new Identifier(MOD_ID, "execute_finisher");
	public static final Identifier START_FINISHER_ANIM_PACKET = new Identifier(MOD_ID, "start_finisher_anim");
	public static final Identifier SYNC_FINISHER_GAUGE_PACKET = new Identifier(MOD_ID, "sync_finisher_gauge");

	@Override
	public void onInitialize() {
		CONFIG = ConfigApiJava.registerAndLoadConfig(RiposteConfig::new);

		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new FinisherLoader());

		Registry.register(Registries.PARTICLE_TYPE, new Identifier(MOD_ID, "parry_trail"), PARRY_TRAIL);
		Registry.register(Registries.PARTICLE_TYPE, new Identifier(MOD_ID, "parry_trail_light"), PARRY_TRAIL_LIGHT);

		Registry.register(Registries.SOUND_EVENT, WEAPON_PARRY_ID, WEAPON_PARRY_SOUND);
		Registry.register(Registries.SOUND_EVENT, NORMAL_PARRY_ID, NORMAL_PARRY_SOUND);
		Registry.register(Registries.SOUND_EVENT, KICK_COMBO_ID, KICK_COMBO_SOUND);
		Registry.register(Registries.SOUND_EVENT, LETHAL_PARRY_ID, LETHAL_PARRY_SOUND);
		Registry.register(Registries.SOUND_EVENT, PARRY_FIST_READY_ID, PARRY_FIST_READY_SOUND);
		Registry.register(Registries.SOUND_EVENT, PARRY_WEAPON_READY_ID, PARRY_WEAPON_READY_SOUND);
		Registry.register(Registries.SOUND_EVENT, PARRY_ROLL_ID, PARRY_ROLL_SOUND);

		Registry.register(Registries.SOUND_EVENT, FINISHER_FIST1_ID, FINISHER_FIST1_SOUND);
		Registry.register(Registries.SOUND_EVENT, FINISHER_FIST2_ID, FINISHER_FIST2_SOUND);
		Registry.register(Registries.SOUND_EVENT, FINISHER_FIST3_ID, FINISHER_FIST3_SOUND);

		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "iron_guard"), IRON_GUARD);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "leather_sock"), LEATHER_SOCK);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "void_guard"), VOID_GUARD);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "copper_guard"), COPPER_GUARD);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "bloodlustful_ring"), BLOODLUSTFUL_RING);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "honorable_cape"), HONORABLE_CAPE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "neural_link"), NEURAL_LINK);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "shulker_head_plate"), SHULKER_HEAD_PLATE);

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
			entries.add(IRON_GUARD);
			entries.add(LEATHER_SOCK);
			entries.add(VOID_GUARD);
			entries.add(COPPER_GUARD);
			entries.add(BLOODLUSTFUL_RING);
			entries.add(HONORABLE_CAPE);
			entries.add(NEURAL_LINK);
			entries.add(SHULKER_HEAD_PLATE);
		});

		ServerPlayNetworking.registerGlobalReceiver(PARRY_SYNC_PACKET, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				if (player instanceof ParryData parryData) {
					int currentCooldown = parryData.getCalculatedCooldown(CONFIG.parryCooldownMs);
					if (parryData.canParry(currentCooldown)) {
						parryData.setParryTimestamp(System.currentTimeMillis());

						ItemStack stack = player.getMainHandStack();
						Item item = stack.getItem();
						boolean isWeapon = item instanceof SwordItem || item instanceof MiningToolItem || item instanceof TridentItem;

						float randomPitch = 1.0f + (player.getWorld().random.nextFloat() - 0.5f) * 0.6f;
						SoundEvent soundToPlay = isWeapon ? PARRY_WEAPON_READY_SOUND : PARRY_FIST_READY_SOUND;

						player.getWorld().playSound(null, player.getBlockPos(), soundToPlay, SoundCategory.PLAYERS, 0.25f, randomPitch);
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(EXECUTE_FINISHER_PACKET, (server, player, handler, buf, responseSender) -> {
			int targetId = buf.readInt();
			server.execute(() -> {
				if (CONFIG.addons.finishers.enableFinishers && player instanceof FinisherData finisherData) {
					if (finisherData.isExecutingFinisher()) return;

					net.minecraft.entity.Entity targetRaw = player.getWorld().getEntityById(targetId);
					if (!(targetRaw instanceof net.minecraft.entity.LivingEntity target)) return;

					String targetEntityId = net.minecraft.registry.Registries.ENTITY_TYPE.getId(target.getType()).toString();
					if (!CONFIG.addons.finishers.isFinisherAllowedFor(targetEntityId)) return;

					ItemStack stack = player.getMainHandStack();
					boolean hasWeapon = stack.getItem() instanceof SwordItem || stack.getItem() instanceof MiningToolItem || stack.getItem() instanceof TridentItem;

					FinisherDefinition def = FinisherLoader.getValidFinisher(target, hasWeapon);
					if (def == null) return;

					boolean canExecute = false;

					if (CONFIG.addons.finishers.finisherMode == RiposteConfig.FinisherMode.GAUGE_METER) {
						if (finisherData.getFinisherGauge(targetId) >= 100f) {
							finisherData.consumeFinisherGauge(targetId, 100f);
							canExecute = true;
						}
					} else {
						if (finisherData.getParryCount(targetId) >= CONFIG.addons.finishers.finisherParryCountMax) {
							finisherData.clearParryCount(targetId);
							canExecute = true;
						}
					}

					if (canExecute) {
						finisherData.startFinisher(targetId, def.id);
					}
				}
			});
		});

		LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
			if (id.equals(LootTables.VILLAGE_WEAPONSMITH_CHEST) || id.equals(LootTables.VILLAGE_ARMORER_CHEST) ||
					id.equals(LootTables.VILLAGE_TOOLSMITH_CHEST) || id.equals(LootTables.STRONGHOLD_CORRIDOR_CHEST) ||
					id.equals(LootTables.RUINED_PORTAL_CHEST)) {
				tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
						.conditionally(RandomChanceLootCondition.builder(0.15f))
						.with(ItemEntry.builder(IRON_GUARD)).build());
			}

			if (id.equals(LootTables.END_CITY_TREASURE_CHEST)) {
				tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
						.conditionally(RandomChanceLootCondition.builder(0.005f))
						.with(ItemEntry.builder(VOID_GUARD)).build());
			}

			if (id.equals(LootTables.STRONGHOLD_CORRIDOR_CHEST) || id.equals(LootTables.STRONGHOLD_CROSSING_CHEST) ||
					id.equals(LootTables.END_CITY_TREASURE_CHEST) || id.equals(LootTables.ANCIENT_CITY_CHEST)) {
				tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
						.conditionally(RandomChanceLootCondition.builder(0.05f))
						.with(ItemEntry.builder(COPPER_GUARD)).build());
			}

			if (id.equals(LootTables.ANCIENT_CITY_CHEST)) {
				tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
						.conditionally(RandomChanceLootCondition.builder(0.08f))
						.with(ItemEntry.builder(NEURAL_LINK)).build());
			}

			if (id.equals(LootTables.END_CITY_TREASURE_CHEST)) {
				tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
						.conditionally(RandomChanceLootCondition.builder(0.05f))
						.with(ItemEntry.builder(SHULKER_HEAD_PLATE)).build());
			}

			Identifier[] genericTreasureChests = new Identifier[]{
					LootTables.SIMPLE_DUNGEON_CHEST, LootTables.ABANDONED_MINESHAFT_CHEST,
					LootTables.DESERT_PYRAMID_CHEST, LootTables.JUNGLE_TEMPLE_CHEST,
					LootTables.BURIED_TREASURE_CHEST, LootTables.SHIPWRECK_TREASURE_CHEST,
					LootTables.ANCIENT_CITY_CHEST, LootTables.END_CITY_TREASURE_CHEST,
					LootTables.BASTION_TREASURE_CHEST, LootTables.STRONGHOLD_CORRIDOR_CHEST
			};

			for (Identifier tableId : genericTreasureChests) {
				if (id.equals(tableId)) {
					tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
							.conditionally(RandomChanceLootCondition.builder(0.03f))
							.with(ItemEntry.builder(BLOODLUSTFUL_RING)).build());

					tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
							.conditionally(RandomChanceLootCondition.builder(0.03f))
							.with(ItemEntry.builder(HONORABLE_CAPE)).build());
				}
			}
		});
	}
}