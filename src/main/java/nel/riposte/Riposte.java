package nel.riposte;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import nel.riposte.config.RiposteConfig;
import nel.riposte.item.RiposteAccessoryItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public static final Item IRON_PLATE = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "parry", null,
			"tooltip.riposte.passive", "tooltip.riposte.passive.projectile", "tooltip.riposte.modifier.recharge_rate_3");

	public static final Item LEATHER_SOCK = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "parry", null,
			"tooltip.riposte.passive", "tooltip.riposte.passive.fall");

	public static final Item CREST_OF_THE_VOID = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "parry", null,
			"tooltip.riposte.passive", "tooltip.riposte.passive.all", "tooltip.riposte.modifier.recharge_rate_3");

	public static final Item COBALT_PLATE = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "parry", null,
			"tooltip.riposte.passive", "tooltip.riposte.passive.projectile", "tooltip.riposte.passive.infinite_kb", "tooltip.riposte.modifier.recharge_rate_5", "tooltip.riposte.modifier.invuln_time");

	public static final Item EVERLASTING_BLOODRING = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "ring", null,
			"tooltip.riposte.passive.bloodring_1", "tooltip.riposte.passive.bloodring_2", "tooltip.riposte.warning.no_stack");

	public static final Item WANDERERS_CAPE = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "cape", null,
			"tooltip.riposte.passive.charge_meter", "tooltip.riposte.passive.damage_dealt");

	public static final Item BRAIN_CHIP = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "hat", null,
			"tooltip.riposte.modifier.recharge_rate_100");

	public static final Item ENDER_DRAGON_SCALE = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "charm", "head/hat",
			"tooltip.riposte.passive.head_slot", "tooltip.riposte.warning.no_stack");

	public static final Identifier PARRY_SYNC_PACKET = new Identifier(MOD_ID, "parry_sync");
	public static final Identifier PARRY_SUCCESS_PACKET = new Identifier(MOD_ID, "parry_success");
	public static final Identifier COMBO_SUCCESS_PACKET = new Identifier(MOD_ID, "combo_success");
	public static final Identifier PARRY_VFX_PACKET = new Identifier(MOD_ID, "parry_vfx");
	public static final Identifier LETHAL_VFX_PACKET = new Identifier(MOD_ID, "lethal_vfx");

	@Override
	public void onInitialize() {
		CONFIG = ConfigApiJava.registerAndLoadConfig(RiposteConfig::new);

		Registry.register(Registries.PARTICLE_TYPE, new Identifier(MOD_ID, "parry_trail"), PARRY_TRAIL);
		Registry.register(Registries.PARTICLE_TYPE, new Identifier(MOD_ID, "parry_trail_light"), PARRY_TRAIL_LIGHT);

		Registry.register(Registries.SOUND_EVENT, WEAPON_PARRY_ID, WEAPON_PARRY_SOUND);
		Registry.register(Registries.SOUND_EVENT, NORMAL_PARRY_ID, NORMAL_PARRY_SOUND);
		Registry.register(Registries.SOUND_EVENT, KICK_COMBO_ID, KICK_COMBO_SOUND);
		Registry.register(Registries.SOUND_EVENT, LETHAL_PARRY_ID, LETHAL_PARRY_SOUND);
		Registry.register(Registries.SOUND_EVENT, PARRY_FIST_READY_ID, PARRY_FIST_READY_SOUND);
		Registry.register(Registries.SOUND_EVENT, PARRY_WEAPON_READY_ID, PARRY_WEAPON_READY_SOUND);

		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "iron_plate"), IRON_PLATE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "leather_sock"), LEATHER_SOCK);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "crest_of_the_void"), CREST_OF_THE_VOID);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "cobalt_plate"), COBALT_PLATE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "everlasting_bloodring"), EVERLASTING_BLOODRING);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "wanderers_cape"), WANDERERS_CAPE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "brain_chip"), BRAIN_CHIP);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "ender_dragon_scale"), ENDER_DRAGON_SCALE);

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
			entries.add(IRON_PLATE);
			entries.add(LEATHER_SOCK);
			entries.add(CREST_OF_THE_VOID);
			entries.add(COBALT_PLATE);
			entries.add(EVERLASTING_BLOODRING);
			entries.add(WANDERERS_CAPE);
			entries.add(BRAIN_CHIP);
			entries.add(ENDER_DRAGON_SCALE);
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

		LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
			if (id.equals(LootTables.VILLAGE_WEAPONSMITH_CHEST) || id.equals(LootTables.VILLAGE_ARMORER_CHEST) ||
					id.equals(LootTables.VILLAGE_TOOLSMITH_CHEST) || id.equals(LootTables.STRONGHOLD_CORRIDOR_CHEST) ||
					id.equals(LootTables.RUINED_PORTAL_CHEST)) {
				tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
						.conditionally(RandomChanceLootCondition.builder(0.15f))
						.with(ItemEntry.builder(IRON_PLATE)).build());
			}

			if (id.equals(LootTables.END_CITY_TREASURE_CHEST)) {
				tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
						.conditionally(RandomChanceLootCondition.builder(0.05f))
						.with(ItemEntry.builder(CREST_OF_THE_VOID)).build());
			}

			if (id.equals(LootTables.STRONGHOLD_CORRIDOR_CHEST) || id.equals(LootTables.STRONGHOLD_CROSSING_CHEST) ||
					id.equals(LootTables.END_CITY_TREASURE_CHEST) || id.equals(LootTables.ANCIENT_CITY_CHEST)) {
				tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
						.conditionally(RandomChanceLootCondition.builder(0.05f))
						.with(ItemEntry.builder(COBALT_PLATE)).build());
			}

			if (id.equals(LootTables.ANCIENT_CITY_CHEST)) {
				tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
						.conditionally(RandomChanceLootCondition.builder(0.08f))
						.with(ItemEntry.builder(BRAIN_CHIP)).build());
			}

			if (id.equals(LootTables.END_CITY_TREASURE_CHEST)) {
				tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
						.conditionally(RandomChanceLootCondition.builder(0.05f))
						.with(ItemEntry.builder(ENDER_DRAGON_SCALE)).build());
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
							.with(ItemEntry.builder(EVERLASTING_BLOODRING)).build());

					tableBuilder.pool(LootPool.builder().rolls(ConstantLootNumberProvider.create(1))
							.conditionally(RandomChanceLootCondition.builder(0.03f))
							.with(ItemEntry.builder(WANDERERS_CAPE)).build());
				}
			}
		});
	}
}