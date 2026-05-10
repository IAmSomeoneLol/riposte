package nel.riposte.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.SimpleAccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
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

public class RiposteClient implements ClientModInitializer {

	private static KeyBinding parryKey;
	public static RiposteClientConfig CLIENT_CONFIG;
	private static final Identifier PARRY_ICON = new Identifier(Riposte.MOD_ID, "textures/gui/cooldown_parry.png");

	@Override
	public void onInitializeClient() {
		CLIENT_CONFIG = ConfigApiJava.registerAndLoadConfig(RiposteClientConfig::new);

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
					((ParryData) client.player).setSuccessfulParryTimestamp(System.currentTimeMillis());
				}
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (parryKey.wasPressed()) {
				if (CLIENT_CONFIG.executionMode == RiposteClientConfig.ExecutionMode.KEYBIND) {
					attemptParry(client);
				}
			}
		});

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient && CLIENT_CONFIG.executionMode == RiposteClientConfig.ExecutionMode.CAMERA) {
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

			if (CLIENT_CONFIG.parryScreenFlash && timeSinceSuccess < 150) {
				float alpha = 1.0f - ((float) timeSinceSuccess / 150.0f);
				int color = ((int) (alpha * 100) << 24) | 0xFFFFFF;

				RenderSystem.enableBlend();
				drawContext.fill(0, 0, screenWidth, screenHeight, color);
				RenderSystem.disableBlend();
			}

			long timeSinceParry = System.currentTimeMillis() - data.getParryTimestamp();
			int currentCooldown = data.getCalculatedCooldown(Riposte.CONFIG.parryCooldownMs);

			if (CLIENT_CONFIG.iconMode == RiposteClientConfig.IconMode.DYNAMIC) {
				if (timeSinceParry > currentCooldown + 1000) {
					return;
				}
			}

			int x = (screenWidth / 2) + CLIENT_CONFIG.iconXOffset;
			int y = (screenHeight / 2) + CLIENT_CONFIG.iconYOffset;

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