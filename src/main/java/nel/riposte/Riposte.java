package nel.riposte;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import nel.riposte.config.RiposteConfig;
import nel.riposte.item.RiposteAccessoryItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Riposte implements ModInitializer {
	public static final String MOD_ID = "riposte";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static RiposteConfig CONFIG;

	public static final Item IRON_PLATE = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "parry",
			"tooltip.riposte.passive", "tooltip.riposte.passive.projectile", "tooltip.riposte.modifier.recharge_rate_3");

	public static final Item LEATHER_SOCK = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "parry",
			"tooltip.riposte.passive", "tooltip.riposte.passive.fall");

	public static final Item CREST_OF_THE_VOID = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "parry",
			"tooltip.riposte.passive", "tooltip.riposte.passive.all", "tooltip.riposte.modifier.recharge_rate_3");

	public static final Item COBALT_PLATE = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "parry",
			"tooltip.riposte.passive", "tooltip.riposte.passive.projectile", "tooltip.riposte.passive.infinite_kb", "tooltip.riposte.modifier.recharge_rate_5", "tooltip.riposte.modifier.invuln_time");

	public static final Item EVERLASTING_BLOODRING = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "ring",
			"tooltip.riposte.passive.bloodring_1", "tooltip.riposte.passive.bloodring_2", "tooltip.riposte.warning.no_stack");

	public static final Item WANDERERS_CAPE = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "necklace",
			"tooltip.riposte.passive.charge_meter", "tooltip.riposte.passive.damage_dealt");

	public static final Item BRAIN_CHIP = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "hat",
			"tooltip.riposte.modifier.recharge_rate_100");

	public static final Item ENDER_DRAGON_SCALE = new RiposteAccessoryItem(new Item.Settings().maxCount(1), "charm",
			"tooltip.riposte.passive.head_slot", "tooltip.riposte.warning.no_stack");

	public static final Identifier PARRY_SYNC_PACKET = new Identifier(MOD_ID, "parry_sync");
	public static final Identifier PARRY_SUCCESS_PACKET = new Identifier(MOD_ID, "parry_success");
	public static final Identifier COMBO_SUCCESS_PACKET = new Identifier(MOD_ID, "combo_success");

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Riposte Parry Mechanics...");
		CONFIG = ConfigApiJava.registerAndLoadConfig(RiposteConfig::new);

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
					}
				}
			});
		});
	}
}