package nel.riposte.item;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.SlotAttributes;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import nel.riposte.Riposte;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

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
    public boolean canEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {        if (stack.isOf(Riposte.BLOODLUSTFUL_RING) || stack.isOf(Riposte.SHULKER_HEAD_PLATE)) {
            var comp = TrinketsApi.getTrinketComponent(entity).orElse(null);
            if (comp != null && comp.isEquipped(stack.getItem())) {
                return false;
            }
        }

        String slotName = slot.inventory().getSlotType().getName();
        String groupName = slot.inventory().getSlotType().getGroup();
        if (this.targetSlot != null) {
            if (this.targetSlot.equals(slotName) || this.targetSlot.equals(groupName + "/" + slotName)) {
                return true;
            }
        }
        return super.canEquip(stack, slot, entity);
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {        if (this.bonusSlot != null && entity instanceof PlayerEntity player && !player.getWorld().isClient) {
            var comp = TrinketsApi.getTrinketComponent(player).orElse(null);
            if (comp != null) {
                String[] groupSlot = this.bonusSlot.split("/");
                if (groupSlot.length == 2) {
                    var group = comp.getInventory().get(groupSlot[0]);
                    if (group != null) {
                        var inv = group.get(groupSlot[1]);
                        if (inv != null && inv.size() > 1) {                            int lastIdx = inv.size() - 1;
                            ItemStack inBonus = inv.getStack(lastIdx);
                            if (!inBonus.isEmpty()) {
                                inv.setStack(lastIdx, ItemStack.EMPTY);
                                player.getInventory().offerOrDrop(inBonus);
                            }
                        }
                    }
                }
            }
        }
        super.onUnequip(stack, slot, entity);
    }

    @Override
    public Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, Identifier id) {
        var modifiers = super.getModifiers(stack, slot, entity, id);
        int bonusAmount = getBonusSlotCount();        if (this.bonusSlot != null && !this.bonusSlot.isEmpty() && bonusAmount > 0) {
            SlotAttributes.addSlotModifier(modifiers, this.bonusSlot, id, bonusAmount, EntityAttributeModifier.Operation.ADD_VALUE);
        }

        return modifiers;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("tooltip.riposte.slot." + this.targetSlot).formatted(Formatting.GOLD));
        super.appendTooltip(stack, context, tooltip, type);
    }
}