package nel.riposte.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import dev.emi.trinkets.api.TrinketsApi;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import nel.riposte.ParryData;
import nel.riposte.Riposte;
import nel.riposte.client.config.RiposteClientConfig;
import nel.riposte.client.mixin.GameRendererInvoker;
import nel.riposte.client.particle.ParryTrailParticle;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.Random;

public class RiposteClient implements ClientModInitializer {

	private static KeyBinding parryKey;
	public static RiposteClientConfig CLIENT_CONFIG;
	private static final Random random = new Random();

	private static final Identifier PARRY_ICON_FULL = new Identifier(Riposte.MOD_ID, "textures/gui/cooldown_parry.png");
	private static final Identifier PARRY_ICON_EMPTY = new Identifier(Riposte.MOD_ID, "textures/gui/cooldown_parry_empty.png");
	private static final Identifier PARRY_ICON_CHARGING = new Identifier(Riposte.MOD_ID, "textures/gui/cooldown_parry_charging.png");

	private static final Identifier CHARGE_SOUND_ID = new Identifier(Riposte.MOD_ID, "cooldown_charge_finish");
	private static final SoundEvent CHARGE_SOUND = SoundEvent.of(CHARGE_SOUND_ID);
	private static boolean wasCharging = false;

	public static long lastLethalParryTimestamp = 0L;
	public static boolean shaderActive = false;

	public static boolean renderLeftArm = false;

	@Override
	public void onInitializeClient() {
		CLIENT_CONFIG = ConfigApiJava.registerAndLoadConfig(RiposteClientConfig::new, RegisterType.CLIENT);

		ParticleFactoryRegistry.getInstance().register(Riposte.PARRY_TRAIL, provider -> new ParryTrailParticle.HeavyFactory(provider));
		ParticleFactoryRegistry.getInstance().register(Riposte.PARRY_TRAIL_LIGHT, provider -> new ParryTrailParticle.LightFactory(provider));

		parryKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.riposte.parry",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_R,
				"category.riposte.keys"
		));

		PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
				new Identifier(Riposte.MOD_ID, "animation"),
				42,
				(AbstractClientPlayerEntity player) -> new ModifierLayer<>()
		);

		TrinketRenderer emptyRenderer = (stack, slotReference, contextModel, matrices, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch) -> {};

		TrinketRendererRegistry.registerRenderer(Riposte.IRON_GUARD, emptyRenderer);
		TrinketRendererRegistry.registerRenderer(Riposte.LEATHER_SOCK, emptyRenderer);
		TrinketRendererRegistry.registerRenderer(Riposte.VOID_GUARD, emptyRenderer);
		TrinketRendererRegistry.registerRenderer(Riposte.COPPER_GUARD, emptyRenderer);
		TrinketRendererRegistry.registerRenderer(Riposte.BLOODLUSTFUL_RING, emptyRenderer);
		TrinketRendererRegistry.registerRenderer(Riposte.HONORABLE_CAPE, emptyRenderer);
		TrinketRendererRegistry.registerRenderer(Riposte.NEURAL_LINK, emptyRenderer);
		TrinketRendererRegistry.registerRenderer(Riposte.SHULKER_HEAD_PLATE, emptyRenderer);

		ClientPlayNetworking.registerGlobalReceiver(Riposte.PARRY_VFX_PACKET, (client, handler, buf, responseSender) -> {
			double px = buf.readDouble();
			double py = buf.readDouble();
			double pz = buf.readDouble();
			float yaw = buf.readFloat();
			boolean isWeapon = buf.readBoolean();
			boolean isHeavyDamage = buf.readBoolean();

			client.execute(() -> {
				if (client.world == null) return;

				if (CLIENT_CONFIG.particleNormal) {
					for (int i = 0; i < 15; i++) {
						double vx = random.nextGaussian() * 0.15;
						double vy = random.nextGaussian() * 0.15;
						double vz = random.nextGaussian() * 0.15;
						client.world.addParticle(ParticleTypes.FIREWORK, px, py, pz, vx, vy, vz);
					}
				}

				if (isWeapon && CLIENT_CONFIG.particleHeavy) {
					DefaultParticleType trailType = isHeavyDamage ? Riposte.PARRY_TRAIL : Riposte.PARRY_TRAIL_LIGHT;

					for (int i = 0; i < 18; i++) {
						double vx = (random.nextDouble() - 0.5) * 3.5;
						double vy = (random.nextDouble() - 0.5) * 3.5;
						double vz = (random.nextDouble() - 0.5) * 3.5;

						client.world.addParticle(trailType, px, py, pz, vx, vy, vz);
					}
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(Riposte.LETHAL_VFX_PACKET, (client, handler, buf, responseSender) -> {
			client.execute(() -> {
				lastLethalParryTimestamp = System.currentTimeMillis();

				if (CLIENT_CONFIG.lethalParryShader && client.gameRenderer != null) {
					((GameRendererInvoker) client.gameRenderer).invokeLoadPostProcessor(new Identifier(Riposte.MOD_ID, "shaders/post/lethal_parry.json"));
					shaderActive = true;
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(Riposte.PARRY_SUCCESS_PACKET, (client, handler, buf, responseSender) -> {
			client.execute(() -> {
				if (client.player != null) {
					ParryData data = (ParryData) client.player;
					data.setSuccessfulParryTimestamp(System.currentTimeMillis());

					var component = TrinketsApi.getTrinketComponent(client.player).orElse(null);
					if (component != null && component.isEquipped(Riposte.HONORABLE_CAPE)) {
						data.refundParryCooldown(Riposte.CONFIG.wanderersCapeCooldownCharge);
					}

					ItemStack stack = client.player.getMainHandStack();
					boolean isWeapon = stack.getItem() instanceof SwordItem || stack.getItem() instanceof MiningToolItem || stack.getItem() instanceof TridentItem;

					String[] weaponAnims = {"parry_weapon", "parry_weapon1", "parry_weapon2", "parry_weapon3"};
					String[] fistAnims = {"parry_fist", "parry_fist1", "parry_fist2"};
					String animName = isWeapon ? weaponAnims[random.nextInt(4)] : fistAnims[random.nextInt(3)];

					playFirstPersonAnimation((AbstractClientPlayerEntity) client.player, animName);
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(Riposte.COMBO_SUCCESS_PACKET, (client, handler, buf, responseSender) -> {
			client.execute(() -> {
				if (client.player != null) {
					ParryData data = (ParryData) client.player;
					data.setSuccessfulComboTimestamp(System.currentTimeMillis());

					playFirstPersonAnimation((AbstractClientPlayerEntity) client.player, "kick_hit");
				}
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (shaderActive) {
				if (!CLIENT_CONFIG.lethalParryShader || System.currentTimeMillis() - lastLethalParryTimestamp > CLIENT_CONFIG.lethalShaderDurationMs) {
					if (client.gameRenderer != null) {
						client.gameRenderer.disablePostProcessor();
					}
					shaderActive = false;
				}
			}

			while (parryKey.wasPressed()) {
				if (CLIENT_CONFIG.parryActivation == RiposteClientConfig.ExecutionMode.KEYBIND) {
					attemptParry(client);
				}
			}
		});

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient && hand == Hand.MAIN_HAND && CLIENT_CONFIG.parryActivation == RiposteClientConfig.ExecutionMode.CAMERA) {
				if (attemptParry(MinecraftClient.getInstance())) {
					return ActionResult.SUCCESS;
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
			int currentCooldown = data.getCalculatedCooldown(Riposte.CONFIG.parryCooldownMs);

			if (CLIENT_CONFIG.iconMode == RiposteClientConfig.IconMode.DYNAMIC) {
				if (timeSinceParry > currentCooldown + 1000) {
					return;
				}
			}

			int baseX = screenWidth / 2;
			int baseY = screenHeight / 2;

			switch (CLIENT_CONFIG.iconAnchor) {
				case TOP_LEFT -> { baseX = 0; baseY = 0; }
				case TOP_CENTER -> { baseX = screenWidth / 2; baseY = 0; }
				case TOP_RIGHT -> { baseX = screenWidth; baseY = 0; }
				case CENTER_LEFT -> { baseX = 0; baseY = screenHeight / 2; }
				case CENTER -> { baseX = screenWidth / 2; baseY = screenHeight / 2; }
				case CENTER_RIGHT -> { baseX = screenWidth; baseY = screenHeight / 2; }
				case BOTTOM_LEFT -> { baseX = 0; baseY = screenHeight; }
				case BOTTOM_CENTER -> { baseX = screenWidth / 2; baseY = screenHeight; }
				case BOTTOM_RIGHT -> { baseX = screenWidth; baseY = screenHeight; }
			}

			int x = baseX + CLIENT_CONFIG.xOffset;
			int y = baseY + CLIENT_CONFIG.yOffset;

			drawContext.getMatrices().push();
			drawContext.getMatrices().translate(x, y, 0);
			drawContext.getMatrices().scale(CLIENT_CONFIG.iconScale, CLIENT_CONFIG.iconScale, 1.0f);

			float progress = Math.min(1.0f, (float) timeSinceParry / currentCooldown);
			int fillHeight = (int) (16 * progress);

			if (progress >= 1.0f) {
				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
				drawContext.drawTexture(PARRY_ICON_FULL, -8, -8, 0, 0, 16, 16, 16, 16);

				if (wasCharging) {
					if (CLIENT_CONFIG.playCooldownSound) {
						float randomPitch = 0.7f + (random.nextFloat() * 0.6f);
						client.getSoundManager().play(PositionedSoundInstance.master(CHARGE_SOUND, randomPitch, 1.0f));
					}
					wasCharging = false;
				}
			} else {
				wasCharging = true;

				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
				drawContext.drawTexture(PARRY_ICON_EMPTY, -8, -8, 0, 0, 16, 16, 16, 16);

				if (fillHeight > 0) {
					drawContext.drawTexture(PARRY_ICON_CHARGING, -8, 8 - fillHeight, 0, 16 - fillHeight, 16, fillHeight, 16, 16);
				}
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

				ItemStack stack = client.player.getMainHandStack();
				boolean isWeapon = stack.getItem() instanceof SwordItem || stack.getItem() instanceof MiningToolItem || stack.getItem() instanceof TridentItem;

				String animName = isWeapon ? "parry_weapon_ready" : "parry_fist_ready";
				playFirstPersonAnimation((AbstractClientPlayerEntity) client.player, animName);

				return true;
			}
		}
		return false;
	}

	private static void playFirstPersonAnimation(AbstractClientPlayerEntity player, String requestedAnimName) {
		String animName = requestedAnimName;
		var animation = PlayerAnimationRegistry.getAnimation(new Identifier(Riposte.MOD_ID, animName));

		if (animation == null && (animName.endsWith("1") || animName.endsWith("2") || animName.endsWith("3"))) {
			animName = animName.substring(0, animName.length() - 1);
			animation = PlayerAnimationRegistry.getAnimation(new Identifier(Riposte.MOD_ID, animName));
		}

		if (animation != null) {
			var animationContainer = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player).get(new Identifier(Riposte.MOD_ID, "animation"));

			if (animationContainer != null) {

				animationContainer.setAnimation(null);

				var keyframePlayer = new KeyframeAnimationPlayer(animation);

				keyframePlayer.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);

				boolean isKick = animName.equals("kick_hit");
				boolean isWeaponAnim = animName.contains("weapon");

				boolean showLeft = isKick || isWeaponAnim;
				renderLeftArm = showLeft;

				keyframePlayer.setFirstPersonConfiguration(new FirstPersonConfiguration(true, showLeft, true, showLeft));

				ModifierLayer<IAnimation> offsetLayer = new ModifierLayer<>();
				offsetLayer.setAnimation(keyframePlayer);

				offsetLayer.addModifierLast(new AbstractModifier() {
					@Override
					public Vec3f get3DTransform(String modelName, TransformType type, float tickDelta, Vec3f value0) {
						Vec3f base = super.get3DTransform(modelName, type, tickDelta, value0);

						// Only apply the pushback if we are in First Person view
						if (type == TransformType.POSITION && MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) {

							if (modelName.equals("rightArm") || modelName.equals("right_arm")) {
								float maxTick = 14.16f;
								float currentTick = keyframePlayer.getTick();
								float animProgress = Math.min(1.0f, Math.max(0.0f, currentTick / maxTick));
								float easeFactor = (float) Math.sin(animProgress * Math.PI);

								return new Vec3f(base.getX(), base.getY(), base.getZ() - (6.0f * easeFactor));
							}

							if (modelName.equals("leftArm") || modelName.equals("left_arm")) {
								float maxTick = 14.16f;
								float currentTick = keyframePlayer.getTick();
								float animProgress = Math.min(1.0f, Math.max(0.0f, currentTick / maxTick));
								float easeFactor = (float) Math.sin(animProgress * Math.PI);

								if (isKick) {
									return new Vec3f(base.getX() + 6.0f, base.getY() + 2.0f, base.getZ() - (6.0f * easeFactor));
								} else {
									return new Vec3f(base.getX(), base.getY(), base.getZ() - (6.0f * easeFactor));
								}
							}
						}
						return base;
					}
				});

				animationContainer.setAnimation(offsetLayer);
			}
		}
	}
}