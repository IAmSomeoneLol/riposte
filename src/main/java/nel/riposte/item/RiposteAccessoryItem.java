package nel.riposte.item;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.SlotAttributes;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class RiposteAccessoryItem extends TrinketItem {

    private final String targetSlot;
    private final String bonusSlot;
    private final String[] tooltipKeys;

    public RiposteAccessoryItem(Settings settings, String targetSlot, String bonusSlot, String... tooltipKeys) {
        super(settings);
        this.targetSlot = targetSlot;
        this.bonusSlot = bonusSlot;
        this.tooltipKeys = tooltipKeys;
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, UUID uuid) {
        var modifiers = super.getModifiers(stack, slot, entity, uuid);

        if (this.bonusSlot != null && !this.bonusSlot.isEmpty()) {
            SlotAttributes.addSlotModifier(modifiers, this.bonusSlot, uuid, 1, EntityAttributeModifier.Operation.ADDITION);
        }

        return modifiers;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (this.tooltipKeys.length > 0) {
            tooltip.add(Text.empty());

            tooltip.add(Text.translatable("tooltip.riposte.slot." + this.targetSlot).formatted(Formatting.GOLD));

            for (String key : tooltipKeys) {
                Formatting color = Formatting.BLUE;

                if (key.contains("debuff")) {
                    color = Formatting.DARK_RED;
                }

                tooltip.add(Text.translatable(key).formatted(color));
            }
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}