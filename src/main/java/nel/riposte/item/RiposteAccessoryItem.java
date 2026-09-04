package nel.riposte.item;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.SlotAttributes;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import nel.riposte.Riposte;
import java.util.List;

public class RiposteAccessoryItem extends TrinketItem {

    private final String targetSlot;
    private final String bonusSlot;

    public RiposteAccessoryItem(Settings settings, String targetSlot, String bonusSlot) {
        super(settings);
        this.targetSlot = targetSlot;
        this.bonusSlot = bonusSlot;
    }

    protected int getBonusSlotCount() {
        return 1;
    }

    @Override
    public Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, Identifier id) {
        var modifiers = super.getModifiers(stack, slot, entity, id);
        int bonusAmount = getBonusSlotCount();

        if (this.bonusSlot != null && !this.bonusSlot.isEmpty() && bonusAmount > 0) {
            Identifier modifierId = Identifier.of(Riposte.MOD_ID, "bonus_slot_" + this.bonusSlot.replace("/", "_"));
            SlotAttributes.addSlotModifier(modifiers, this.bonusSlot, modifierId, bonusAmount, EntityAttributeModifier.Operation.ADD_VALUE);
        }

        return modifiers;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.riposte.slot." + this.targetSlot).formatted(Formatting.GOLD));
        super.appendTooltip(stack, context, tooltip, type);
    }
}