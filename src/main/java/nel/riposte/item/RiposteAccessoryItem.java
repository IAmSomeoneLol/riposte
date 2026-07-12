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

    public RiposteAccessoryItem(Settings settings, String targetSlot, String bonusSlot) {
        super(settings);
        this.targetSlot = targetSlot;
        this.bonusSlot = bonusSlot;
    }

    protected int getBonusSlotCount() {
        return 1;
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, UUID uuid) {
        var modifiers = super.getModifiers(stack, slot, entity, uuid);
        int bonusAmount = getBonusSlotCount();

        if (this.bonusSlot != null && !this.bonusSlot.isEmpty() && bonusAmount > 0) {
            SlotAttributes.addSlotModifier(modifiers, this.bonusSlot, uuid, bonusAmount, EntityAttributeModifier.Operation.ADDITION);
        }

        return modifiers;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.riposte.slot." + this.targetSlot).formatted(Formatting.GOLD));
        super.appendTooltip(stack, world, tooltip, context);
    }
}