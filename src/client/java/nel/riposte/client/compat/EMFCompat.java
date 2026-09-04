package nel.riposte.client.compat;

import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import nel.riposte.Riposte;
import nel.riposte.client.RiposteClient;
import nel.riposte.client.config.RiposteClientConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.util.function.Function;

public class EMFCompat {

    public static void init() {
        try {
            Class<?> emfApiClass = Class.forName("traben.entity_model_features.EMFAnimationApi");
            Method registerMethod = emfApiClass.getMethod("registerPauseCondition", Function.class);

            Function<Object, Boolean> pauseCondition = (entityObj) -> {

                RiposteClientConfig.AnimationMaskingMode mode = RiposteClient.CLIENT_CONFIG.addons.experimental.animationMasking;


                if (mode == RiposteClientConfig.AnimationMaskingMode.OFF) return false;

                if (entityObj instanceof AbstractClientPlayerEntity player) {
                    var anim = PlayerAnimationAccess.getPlayerAssociatedData(player).get(new Identifier(Riposte.MOD_ID, "animation"));

                    if (anim != null && anim.isActive()) {

                        if (mode == RiposteClientConfig.AnimationMaskingMode.FULL) {
                            return true;
                        }


                        String animName = RiposteClient.currentParryAnimation;
                        if (animName != null && (animName.contains("finisher") || animName.contains("execute") || animName.contains("kick"))) {
                            return true;
                        }
                    }
                }
                return false;
            };

            registerMethod.invoke(null, pauseCondition);
            Riposte.LOGGER.info("Successfully registered Configurable EMF Animation Pause Condition for Riposte.");

        } catch (Throwable e) {
            Riposte.LOGGER.debug("EMF Animation API not found or unsupported version. Skipping compat.");
        }
    }
}