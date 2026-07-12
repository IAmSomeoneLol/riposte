package nel.riposte;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FinisherLoader extends SinglePreparationResourceReloader<List<FinisherDefinition>> implements IdentifiableResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final List<FinisherDefinition> REGISTRY = new ArrayList<>();
    private static final Random RANDOM = new Random();

    @Override
    public Identifier getFabricId() {
        return new Identifier(Riposte.MOD_ID, "finisher_loader");
    }

    @Override
    protected List<FinisherDefinition> prepare(ResourceManager manager, Profiler profiler) {
        List<FinisherDefinition> definitions = new ArrayList<>();

        for (Map.Entry<Identifier, net.minecraft.resource.Resource> entry : manager.findResources("finishers", id -> id.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = new InputStreamReader(entry.getValue().getInputStream())) {
                FinisherDefinition def = GSON.fromJson(reader, FinisherDefinition.class);
                if (def != null) definitions.add(def);
            } catch (Exception e) {
                Riposte.LOGGER.error("Failed to load finisher logic JSON: " + entry.getKey(), e);
            }
        }
        return definitions;
    }

    @Override
    protected void apply(List<FinisherDefinition> prepared, ResourceManager manager, Profiler profiler) {
        REGISTRY.clear();
        REGISTRY.addAll(prepared);
        Riposte.LOGGER.info("Successfully loaded " + REGISTRY.size() + " data-driven finishers!");
    }

    public static String evaluateSize(LivingEntity entity) {
        float height = entity.getHeight();
        float width = entity.getWidth();

        if (height < 0.9f || (width > 1.2f && height < 1.2f)) return "SMALL";
        if (height > 2.3f || width > 1.4f) return "LARGE";
        return "MEDIUM";
    }

    public static FinisherDefinition getValidFinisher(LivingEntity target, boolean hasWeapon) {
        String targetSize = evaluateSize(target);
        boolean isGrounded = target.isOnGround();

        List<FinisherDefinition> valid = REGISTRY.stream()
                // Modified to accept exact matches OR the "ALL" fallback
                .filter(def -> def.target_size != null && (def.target_size.equalsIgnoreCase(targetSize) || def.target_size.equalsIgnoreCase("ALL")))
                .filter(def -> !def.requires_grounded || isGrounded)
                .filter(def -> def.weapon_requirement == null ||
                        def.weapon_requirement.equalsIgnoreCase("ANY") ||
                        (def.weapon_requirement.equalsIgnoreCase("WEAPON") && hasWeapon) ||
                        (def.weapon_requirement.equalsIgnoreCase("FIST") && !hasWeapon))
                .toList();

        if (valid.isEmpty()) return null;
        return valid.get(RANDOM.nextInt(valid.size()));
    }

    public static FinisherDefinition getFinisherById(String id) {
        return REGISTRY.stream().filter(def -> def.id.equals(id)).findFirst().orElse(null);
    }
}