package nel.riposte.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import dev.emi.trinkets.api.TrinketsApi;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import nel.riposte.FinisherData;
import nel.riposte.FinisherDefinition;
import nel.riposte.FinisherLoader;
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
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.TridentItem;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import org.joml.Matrix4f;

import java.io.InputStream;
import java.util.Random;
import java.util.UUID;

public class RiposteClient implements ClientModInitializer {

	private static KeyBinding parryKey;
	private static KeyBinding finisherKey;
	public static RiposteClientConfig CLIENT_CONFIG;
	private static final Random random = new Random();

	private static final Identifier PARRY_ICON_FULL = new Identifier(Riposte.MOD_ID, "textures/gui/cooldown_parry.png");
	private static final Identifier PARRY_ICON_EMPTY = new Identifier(Riposte.MOD_ID, "textures/gui/cooldown_parry_empty.png");
	private static final Identifier PARRY_ICON_CHARGING = new Identifier(Riposte.MOD_ID, "textures/gui/cooldown_parry_charging.png");

	private static final Identifier FINISHER_PROMPT_BG = new Identifier(Riposte.MOD_ID, "textures/gui/finisher_prompt.png");

	private static final Identifier CHARGE_SOUND_ID = new Identifier(Riposte.MOD_ID, "cooldown_charge_finish");
	private static final SoundEvent CHARGE_SOUND = SoundEvent.of(CHARGE_SOUND_ID);
	private static boolean wasCharging = false;

	public static long lastLethalParryTimestamp = 0L;
	public static boolean shaderActive = false;
	public static boolean renderLeftArm = false;

	public static boolean lastParryWasFall = false;

	public static long finisherStartTime = 0;
	public static long finisherEndTime = 0;
	public static float originalYaw = 0;
	public static float originalPitch = 0;
	public static float lockedYaw = 0;
	public static float lockedPitch = 0;
	public static boolean wasExecuting = false;

	// --- NEW: Syncs the JSON Timeline Flash with the HUD Render! ---
	public static long finisherFlashTimestamp = 0L;
	private static int lastFlashTick = -1;

	private static int renderTopPadding = 0;
	private static int renderBottomPadding = 0;

	public static String currentParryAnimation = "";

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

		finisherKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.riposte.finisher",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
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

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return new Identifier(Riposte.MOD_ID, "icon_padding_calculator");
			}

			@Override
			public void reload(ResourceManager manager) {
				try {
					// Padding logic
				} catch (Exception e) {
					renderTopPadding = 0;
					renderBottomPadding = 0;
				}
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(Riposte.PARRY_VFX_PACKET, (client, handler, buf, responseSender) -> {
			double px = buf.readDouble();
			double py = buf.readDouble();
			double pz = buf.readDouble();
			boolean isWeapon = buf.readBoolean();
			boolean isHeavyDamage = buf.readBoolean();

			client.execute(() -> {
				if (client.world == null) return;
				if (CLIENT_CONFIG.particleNormal) {
					for (int i = 0; i < 15; i++) client.world.addParticle(ParticleTypes.FIREWORK, px, py, pz, random.nextGaussian() * 0.15, random.nextGaussian() * 0.15, random.nextGaussian() * 0.15);
				}
				if (isWeapon && CLIENT_CONFIG.particleHeavy) {
					DefaultParticleType trailType = isHeavyDamage ? Riposte.PARRY_TRAIL : Riposte.PARRY_TRAIL_LIGHT;
					for (int i = 0; i < 18; i++) client.world.addParticle(trailType, px, py, pz, (random.nextDouble() - 0.5) * 3.5, (random.nextDouble() - 0.5) * 3.5, (random.nextDouble() - 0.5) * 3.5);
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

		ClientPlayNetworking.registerGlobalReceiver(Riposte.SYNC_FINISHER_GAUGE_PACKET, (client, handler, buf, responseSender) -> {
			int targetId = buf.readInt();
			float gauge = buf.readFloat();
			int parryCount = buf.readInt();

			client.execute(() -> {
				if (client.player != null) {
					FinisherData fData = (FinisherData) client.player;
					fData.setFinisherGauge(targetId, gauge);
					fData.setParryCount(targetId, parryCount);
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(Riposte.PARRY_SUCCESS_PACKET, (client, handler, buf, responseSender) -> {
			boolean isFallParry = buf.readBoolean();
			client.execute(() -> {
				if (client.player != null) {
					ParryData data = (ParryData) client.player;
					long now = System.currentTimeMillis();
					data.setSuccessfulParryTimestamp(now);
					lastParryWasFall = isFallParry;
					if (isFallParry) data.setParryTimestamp(now);

					var component = TrinketsApi.getTrinketComponent(client.player).orElse(null);
					if (component != null && component.isEquipped(Riposte.HONORABLE_CAPE)) data.refundParryCooldown(Riposte.CONFIG.wanderersCapeCooldownCharge);

					String animName;
					if (isFallParry) animName = "parry_fall_damage";
					else {
						ItemStack stack = client.player.getMainHandStack();
						boolean isWeapon = stack.getItem() instanceof SwordItem || stack.getItem() instanceof MiningToolItem || stack.getItem() instanceof TridentItem;
						animName = isWeapon ? new String[]{"parry_weapon", "parry_weapon1", "parry_weapon2", "parry_weapon3"}[random.nextInt(4)] : new String[]{"parry_fist", "parry_fist1", "parry_fist2"}[random.nextInt(3)];
					}
					playFirstPersonAnimation((AbstractClientPlayerEntity) client.player, animName);
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(Riposte.COMBO_SUCCESS_PACKET, (client, handler, buf, responseSender) -> {
			client.execute(() -> {
				if (client.player != null) {
					((ParryData) client.player).setSuccessfulComboTimestamp(System.currentTimeMillis());
					lastParryWasFall = false;
					ItemStack stack = client.player.getMainHandStack();
					boolean isWeapon = stack.getItem() instanceof SwordItem || stack.getItem() instanceof MiningToolItem || stack.getItem() instanceof TridentItem;
					playFirstPersonAnimation((AbstractClientPlayerEntity) client.player, isWeapon ? "weapon_kick_hit" : "kick_hit");
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(Riposte.START_FINISHER_ANIM_PACKET, (client, handler, buf, responseSender) -> {
			UUID playerUuid = buf.readUuid();
			int targetId = buf.readInt();
			String finisherId = buf.readString();

			client.execute(() -> {
				if (client.world != null) {
					PlayerEntity animPlayer = client.world.getPlayerByUuid(playerUuid);
					if (animPlayer instanceof AbstractClientPlayerEntity clientPlayer) {

						FinisherDefinition def = FinisherLoader.getFinisherById(finisherId);
						if (def == null) return;

						if (animPlayer == client.player) {
							finisherStartTime = System.currentTimeMillis();
							originalYaw = client.player.getYaw();
							originalPitch = client.player.getPitch();
							wasExecuting = true;

							FinisherData fData = (FinisherData) client.player;
							if (Riposte.CONFIG.addons.finishers.finisherMode == nel.riposte.config.RiposteConfig.FinisherMode.GAUGE_METER) {
								fData.consumeFinisherGauge(targetId, 100f);
							} else {
								fData.clearParryCount(targetId);
							}
						}

						((FinisherData) clientPlayer).setActiveFinisherId(finisherId);
						((FinisherData) clientPlayer).startFinisher(targetId, finisherId);

						playFirstPersonAnimation(clientPlayer, def.animation_id);
					}
				}
			});
		});

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				FinisherData fData = (FinisherData) client.player;
				boolean isExecuting = fData.isExecutingFinisher();

				if (!isExecuting && wasExecuting) {
					finisherEndTime = System.currentTimeMillis();
					lockedYaw = client.player.getYaw();
					lockedPitch = client.player.getPitch();
					wasExecuting = false;
				}

				if (isExecuting) {
					client.options.forwardKey.setPressed(false);
					client.options.backKey.setPressed(false);
					client.options.leftKey.setPressed(false);
					client.options.rightKey.setPressed(false);
					client.options.jumpKey.setPressed(false);
					client.options.sneakKey.setPressed(false);
					client.options.attackKey.setPressed(false);
					client.options.useKey.setPressed(false);
				}
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (shaderActive) {
				if (!CLIENT_CONFIG.lethalParryShader || System.currentTimeMillis() - lastLethalParryTimestamp > CLIENT_CONFIG.lethalShaderDurationMs) {
					if (client.gameRenderer != null) client.gameRenderer.disablePostProcessor();
					shaderActive = false;
				}
			}

			if (client.player != null) {

				// --- NEW: CATCHES THE "FLASH" EVENT FROM THE JSON TIMELINE ---
				FinisherData fData = (FinisherData) client.player;
				if (fData.isExecutingFinisher()) {
					FinisherDefinition def = FinisherLoader.getFinisherById(fData.getActiveFinisherId());
					if (def != null && def.timeline != null) {
						for (FinisherDefinition.TimelineEvent event : def.timeline) {
							if (event.tick == fData.getFinisherTick() && "flash".equals(event.action)) {
								if (lastFlashTick != fData.getFinisherTick()) {
									finisherFlashTimestamp = System.currentTimeMillis();
									lastFlashTick = fData.getFinisherTick();
								}
							}
						}
					}
				} else {
					lastFlashTick = -1;
				}

				ParryData data = (ParryData) client.player;
				int currentWindow = data.getCalculatedWindow(Riposte.CONFIG.parryWindowMs);

				if (!data.isParryActive(currentWindow)) {
					if (currentParryAnimation.equals("parry_fist_ready") || currentParryAnimation.equals("parry_weapon_ready")) {
						if (client.options.attackKey.isPressed() || client.options.useKey.isPressed()) {
							var animationContainer = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(client.player).get(new Identifier(Riposte.MOD_ID, "animation"));
							if (animationContainer != null) {
								animationContainer.setAnimation(null);
								currentParryAnimation = "";
							}
						}
					}
				}
			}

			while (parryKey.wasPressed()) {
				if (CLIENT_CONFIG.parryActivation == RiposteClientConfig.ExecutionMode.KEYBIND) attemptParry(client);
			}

			while (finisherKey.wasPressed()) {
				if (client.player != null && client.world != null) {
					FinisherData finisherData = (FinisherData) client.player;

					if (finisherData.isExecutingFinisher()) continue;

					Box box = client.player.getBoundingBox().expand(6.0);
					Entity closestValid = null;
					double closestDist = Double.MAX_VALUE;

					Vec3d lookVec = client.player.getRotationVec(1.0F).normalize();

					for (Entity target : client.world.getEntitiesByClass(LivingEntity.class, box, e -> e != client.player)) {
						double dist = client.player.distanceTo(target);
						if (dist > 6.0f) continue;

						Vec3d toTarget = target.getPos().subtract(client.player.getPos()).normalize();
						if (lookVec.dotProduct(toTarget) < 0.5) continue;

						String entityId = net.minecraft.registry.Registries.ENTITY_TYPE.getId(target.getType()).toString();
						if (!Riposte.CONFIG.addons.finishers.finisherWhitelistMobs.contains(entityId)) continue;

						boolean canExecute = false;
						if (Riposte.CONFIG.addons.finishers.finisherMode == nel.riposte.config.RiposteConfig.FinisherMode.GAUGE_METER) {
							if (finisherData.getFinisherGauge(target.getId()) >= 100f) canExecute = true;
						} else {
							if (finisherData.getParryCount(target.getId()) >= Riposte.CONFIG.addons.finishers.finisherParryCountMax) canExecute = true;
						}

						if (canExecute && dist < closestDist) {
							closestValid = target;
							closestDist = dist;
						}
					}

					if (closestValid != null) {
						PacketByteBuf buf = PacketByteBufs.create();
						buf.writeInt(closestValid.getId());
						ClientPlayNetworking.send(Riposte.EXECUTE_FINISHER_PACKET, buf);
					}
				}
			}
		});

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient && hand == Hand.MAIN_HAND && CLIENT_CONFIG.parryActivation == RiposteClientConfig.ExecutionMode.CAMERA) {
				if (attemptParry(MinecraftClient.getInstance())) return ActionResult.SUCCESS;
			}
			return ActionResult.PASS;
		});

		WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null || client.world == null || !Riposte.CONFIG.addons.finishers.enableFinishers) return;

			FinisherData finisherData = (FinisherData) client.player;
			if (finisherData.isExecutingFinisher()) return;

			Box box = client.player.getBoundingBox().expand(6.0);
			Vec3d lookVec = client.player.getRotationVec(1.0F).normalize();

			for (Entity target : client.world.getEntitiesByClass(LivingEntity.class, box, e -> e != client.player)) {
				if (client.player.distanceTo(target) > 6.0f) continue;

				Vec3d toTarget = target.getPos().subtract(client.player.getPos()).normalize();
				if (lookVec.dotProduct(toTarget) < 0.5) continue;

				String entityId = net.minecraft.registry.Registries.ENTITY_TYPE.getId(target.getType()).toString();

				if (Riposte.CONFIG.addons.finishers.finisherWhitelistMobs.contains(entityId)) {
					boolean canExecute = false;

					if (Riposte.CONFIG.addons.finishers.finisherMode == nel.riposte.config.RiposteConfig.FinisherMode.GAUGE_METER) {
						if (finisherData.getFinisherGauge(target.getId()) >= 100f) canExecute = true;
					} else {
						if (finisherData.getParryCount(target.getId()) >= Riposte.CONFIG.addons.finishers.finisherParryCountMax) canExecute = true;
					}

					if (canExecute) {
						net.minecraft.client.util.math.MatrixStack matrices = context.matrixStack();
						net.minecraft.client.render.Camera camera = context.camera();

						double x = net.minecraft.util.math.MathHelper.lerp(context.tickDelta(), target.lastRenderX, target.getX()) - camera.getPos().x;
						double y = net.minecraft.util.math.MathHelper.lerp(context.tickDelta(), target.lastRenderY, target.getY()) + (target.getHeight() * 0.6f) - camera.getPos().y;
						double z = net.minecraft.util.math.MathHelper.lerp(context.tickDelta(), target.lastRenderZ, target.getZ()) - camera.getPos().z;

						matrices.push();
						matrices.translate(x, y, z);
						matrices.multiply(client.gameRenderer.getCamera().getRotation());

						float scale = CLIENT_CONFIG.addons.finishers.contextualButtonPromptSize;
						matrices.scale(-scale, -scale, scale);

						Matrix4f matrix4f = matrices.peek().getPositionMatrix();
						int light = 15728880;

						net.minecraft.client.render.VertexConsumer consumer = context.consumers().getBuffer(net.minecraft.client.render.RenderLayer.getTextSeeThrough(FINISHER_PROMPT_BG));
						float texSize = 16f;

						consumer.vertex(matrix4f, -texSize, -texSize, 0).color(255, 255, 255, 255).texture(0f, 0f).light(light).next();
						consumer.vertex(matrix4f, -texSize, texSize, 0).color(255, 255, 255, 255).texture(0f, 1f).light(light).next();
						consumer.vertex(matrix4f, texSize, texSize, 0).color(255, 255, 255, 255).texture(1f, 1f).light(light).next();
						consumer.vertex(matrix4f, texSize, -texSize, 0).color(255, 255, 255, 255).texture(1f, 0f).light(light).next();

						String keyName = finisherKey.getBoundKeyLocalizedText().getString().toUpperCase();
						float textWidth = client.textRenderer.getWidth(keyName);

						client.textRenderer.draw(keyName, -textWidth / 2f, -4f, 0xFFFFFF, false, matrix4f, context.consumers(), net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH, 0x00000000, light);

						matrices.pop();
					}
				}
			}
		});

		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player == null) return;

			ParryData data = (ParryData) client.player;
			int screenWidth = client.getWindow().getScaledWidth();
			int screenHeight = client.getWindow().getScaledHeight();

			long timeSinceSuccess = System.currentTimeMillis() - data.getSuccessfulParryTimestamp();
			long timeSinceCombo = System.currentTimeMillis() - data.getSuccessfulComboTimestamp();
			long timeSinceTimelineFlash = System.currentTimeMillis() - finisherFlashTimestamp; // NEW

			long timeSinceFlash = Math.min(timeSinceSuccess, timeSinceCombo);
			timeSinceFlash = Math.min(timeSinceFlash, timeSinceTimelineFlash); // MERGES THE FLASH EVENTS!

			if (CLIENT_CONFIG.screenFlash && timeSinceFlash < CLIENT_CONFIG.screenFlashDurationMs && !lastParryWasFall) {
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
				if (timeSinceParry > currentCooldown + 1000) return;
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
			int visibleHeight = Math.max(1, 16 - renderTopPadding - renderBottomPadding);
			int fillHeight = renderBottomPadding + Math.round(visibleHeight * progress);

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
				if (fillHeight > 0) drawContext.drawTexture(PARRY_ICON_CHARGING, -8, 8 - fillHeight, 0, 16 - fillHeight, 16, fillHeight, 16, 16);
			}

			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
			drawContext.getMatrices().pop();
		});
	}

	private boolean attemptParry(MinecraftClient client) {
		if (client.player != null) {
			if (((FinisherData) client.player).isExecutingFinisher()) return false;

			ParryData data = (ParryData) client.player;
			int currentCooldown = data.getCalculatedCooldown(Riposte.CONFIG.parryCooldownMs);

			if (data.canParry(currentCooldown)) {
				data.setParryTimestamp(System.currentTimeMillis());
				ClientPlayNetworking.send(Riposte.PARRY_SYNC_PACKET, PacketByteBufs.create());

				lastParryWasFall = false;
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
		currentParryAnimation = animName;
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

				boolean isKick = animName.contains("kick_hit");
				boolean isWeaponAnim = animName.contains("weapon");
				boolean isFall = animName.contains("fall_damage");
				boolean isFinisher = animName.contains("finisher");

				boolean showLeft = isKick || isWeaponAnim || isFall || isFinisher || animName.contains("execute");
				renderLeftArm = showLeft;

				keyframePlayer.setFirstPersonConfiguration(new FirstPersonConfiguration(true, showLeft, true, showLeft));
				animationContainer.setAnimation(keyframePlayer);
			}
		}
	}
}