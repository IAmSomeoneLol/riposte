package nel.riposte.item;

import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RiposteAccessoryItem extends Item implements Accessory {

    private final String targetSlot;
    private final String[] tooltipKeys;

    public RiposteAccessoryItem(Settings settings, String targetSlot, String... tooltipKeys) {
        super(settings);
        this.targetSlot = targetSlot;
        this.tooltipKeys = tooltipKeys;
    }

    @Override
    public boolean canEquip(ItemStack stack, SlotReference reference) {
        return reference.slotName().equals(this.targetSlot);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (this.tooltipKeys.length > 0) {
            tooltip.add(Text.empty());

            // Appends the raw target slot name directly into the translation key
            tooltip.add(Text.translatable("tooltip.riposte.slot." + this.targetSlot).formatted(Formatting.GRAY));

            for (String key : tooltipKeys) {
                tooltip.add(Text.translatable(key).formatted(Formatting.BLUE));
            }
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}