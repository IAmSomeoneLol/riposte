package nel.riposte.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.SimpleAccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import nel.riposte.ParryData;
import nel.riposte.Riposte;
import nel.riposte.client.config.RiposteClientConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class RiposteClient implements ClientModInitializer {

	private static KeyBinding parryKey;
	public static RiposteClientConfig CLIENT_CONFIG;
	private static final Identifier PARRY_ICON = new Identifier(Riposte.MOD_ID, "textures/gui/cooldown_parry.png");

	@Override
	public void onInitializeClient() {
		CLIENT_CONFIG = ConfigApiJava.registerAndLoadConfig(RiposteClientConfig::new, RegisterType.CLIENT);

		parryKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.riposte.parry",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_R,
				"category.riposte.keys"
		));

		SimpleAccessoryRenderer emptyRenderer = new SimpleAccessoryRenderer() {
			@Override
			public <M extends LivingEntity> void align(ItemStack stack, SlotReference reference, EntityModel<M> model, MatrixStack matrices) {}

			@Override
			public <M extends LivingEntity> void render(ItemStack stack, SlotReference reference, MatrixStack matrices, EntityModel<M> model, VertexConsumerProvider multiBufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {}
		};

		AccessoriesRendererRegistry.registerRenderer(Riposte.IRON_PLATE, () -> emptyRenderer);
		AccessoriesRendererRegistry.registerRenderer(Riposte.LEATHER_SOCK, () -> emptyRenderer);
		AccessoriesRendererRegistry.registerRenderer(Riposte.CREST_OF_THE_VOID, () -> emptyRenderer);
		AccessoriesRendererRegistry.registerRenderer(Riposte.COBALT_PLATE, () -> emptyRenderer);
		AccessoriesRendererRegistry.registerRenderer(Riposte.EVERLASTING_BLOODRING, () -> emptyRenderer);
		AccessoriesRendererRegistry.registerRenderer(Riposte.WANDERERS_CAPE, () -> emptyRenderer);
		AccessoriesRendererRegistry.registerRenderer(Riposte.BRAIN_CHIP, () -> emptyRenderer);
		AccessoriesRendererRegistry.registerRenderer(Riposte.ENDER_DRAGON_SCALE, () -> emptyRenderer);

		ClientPlayNetworking.registerGlobalReceiver(Riposte.PARRY_SUCCESS_PACKET, (client, handler, buf, responseSender) -> {
			client.execute(() -> {
				if (client.player != null) {
					ParryData data = (ParryData) client.player;
					data.setSuccessfulParryTimestamp(System.currentTimeMillis());

					var capability = io.wispforest.accessories.api.AccessoriesCapability.get(client.player);
					if (capability != null && capability.isEquipped(Riposte.WANDERERS_CAPE)) {
						// Flattened reference here
						data.refundParryCooldown(Riposte.CONFIG.wanderersCapeCooldownCharge);
					}
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(Riposte.COMBO_SUCCESS_PACKET, (client, handler, buf, responseSender) -> {
			client.execute(() -> {
				if (client.player != null) {
					ParryData data = (ParryData) client.player;
					data.setSuccessfulComboTimestamp(System.currentTimeMillis());

					var animation = PlayerAnimationRegistry.getAnimation(new Identifier(Riposte.MOD_ID, "combo_kick"));
					if (animation != null) {
						ModifierLayer<dev.kosmx.playerAnim.api.layered.IAnimation> animationContainer =
								(ModifierLayer<dev.kosmx.playerAnim.api.layered.IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData((AbstractClientPlayerEntity) client.player).get(new Identifier(Riposte.MOD_ID, "animation"));

						if (animationContainer != null) {
							animationContainer.setAnimation(new KeyframeAnimationPlayer(animation));
						}
					}
				}
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (parryKey.wasPressed()) {
				if (CLIENT_CONFIG.parryActivation == RiposteClientConfig.ExecutionMode.KEYBIND) {
					attemptParry(client);
				}
			}
		});

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient && CLIENT_CONFIG.parryActivation == RiposteClientConfig.ExecutionMode.CAMERA) {
				MinecraftClient client = MinecraftClient.getInstance();
				if (attemptParry(client)) {
					return ActionResult.CONSUME;
				}
			}
			return ActionResult.PASS;
		});

		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null) return;

			ParryData data = (ParryData) client.player;
			int screenWidth = client.getWindow().getScaledWidth();
			int screenHeight = client.getWindow().getScaledHeight();

			long timeSinceSuccess = System.currentTimeMillis() - data.getSuccessfulParryTimestamp();
			long timeSinceCombo = System.currentTimeMillis() - data.getSuccessfulComboTimestamp();
			long timeSinceFlash = Math.min(timeSinceSuccess, timeSinceCombo);

			if (CLIENT_CONFIG.screenFlash && timeSinceFlash < CLIENT_CONFIG.screenFlashDurationMs) {
				float alpha = 1.0f - ((float) timeSinceFlash / CLIENT_CONFIG.screenFlashDurationMs);
				int rgb = CLIENT_CONFIG.screenFlashType == RiposteClientConfig.FlashType.WHITE ? 0xFFFFFF : 0x000000;
				int color = ((int) (alpha * 100) << 24) | rgb;

				RenderSystem.enableBlend();
				drawContext.fill(0, 0, screenWidth, screenHeight, color);
				RenderSystem.disableBlend();
			}

			long timeSinceParry = System.currentTimeMillis() - data.getParryTimestamp();
			// Flattened reference here
			int currentCooldown = data.getCalculatedCooldown(Riposte.CONFIG.parryCooldownMs);

			if (CLIENT_CONFIG.iconMode == RiposteClientConfig.IconMode.DYNAMIC) {
				if (timeSinceParry > currentCooldown + 1000) {
					return;
				}
			}

			int x = (screenWidth / 2) + CLIENT_CONFIG.xOffset;
			int y = (screenHeight / 2) + CLIENT_CONFIG.yOffset;

			drawContext.getMatrices().push();
			drawContext.getMatrices().translate(x, y, 0);
			drawContext.getMatrices().scale(CLIENT_CONFIG.iconScale, CLIENT_CONFIG.iconScale, 1.0f);

			float progress = Math.min(1.0f, (float) timeSinceParry / currentCooldown);
			int fillHeight = (int) (16 * progress);

			RenderSystem.setShaderColor(0.15f, 0.15f, 0.15f, 0.7f);
			drawContext.drawTexture(PARRY_ICON, -8, -8, 0, 0, 16, 16, 16, 16);

			if (fillHeight > 0) {
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
				drawContext.drawTexture(PARRY_ICON, -8, 8 - fillHeight, 0, 16 - fillHeight, 16, fillHeight, 16, 16);
			}

			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			drawContext.getMatrices().pop();
		});
	}

	private boolean attemptParry(MinecraftClient client) {
		if (client.player != null) {
			ParryData data = (ParryData) client.player;

			// Flattened reference here
			int currentCooldown = data.getCalculatedCooldown(Riposte.CONFIG.parryCooldownMs);

			if (data.canParry(currentCooldown)) {
				data.setParryTimestamp(System.currentTimeMillis());
				ClientPlayNetworking.send(Riposte.PARRY_SYNC_PACKET, PacketByteBufs.create());
				return true;
			}
		}
		return false;
	}
}