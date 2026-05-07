package nel.riposte;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import nel.riposte.config.RiposteConfig;
import nel.riposte.item.RiposteAccessoryItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Riposte implements ModInitializer {
	public static final String MOD_ID = "riposte";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static RiposteConfig CONFIG;

	// Passing the exact tooltips to the constructor!
	public static final Item IRON_PLATE = new RiposteAccessoryItem(
			new Item.Settings().maxCount(1),
			"tooltip.riposte.passive.projectile",
			"tooltip.riposte.modifier.recharge_rate"
	);

	public static final Item LEATHER_SOCK = new RiposteAccessoryItem(
			new Item.Settings().maxCount(1),
			"tooltip.riposte.passive.fall"
	);

	public static final Identifier PARRY_SYNC_PACKET = new Identifier(MOD_ID, "parry_sync");

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Riposte Parry Mechanics...");

		CONFIG = ConfigApiJava.registerAndLoadConfig(RiposteConfig::new);

		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "iron_plate"), IRON_PLATE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "leather_sock"), LEATHER_SOCK);

		ServerPlayNetworking.registerGlobalReceiver(PARRY_SYNC_PACKET, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				if (player instanceof ParryData parryData) {
					if (parryData.canParry(CONFIG.parryCooldownMs)) {
						parryData.setParryTimestamp(System.currentTimeMillis());
					}
				}
			});
		});
	}
}