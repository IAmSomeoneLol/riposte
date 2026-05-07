package nel.riposte.item;

import io.wispforest.accessories.api.Accessory;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RiposteAccessoryItem extends Item implements Accessory {

    private final String[] tooltipKeys;

    // Custom constructor that accepts our specific design doc attributes
    public RiposteAccessoryItem(Settings settings, String... tooltipKeys) {
        super(settings);
        this.tooltipKeys = tooltipKeys;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (this.tooltipKeys.length > 0) {
            // Adds a blank line for spacing
            tooltip.add(Text.empty());

            // Adds the Gray slot indicator
            tooltip.add(Text.translatable("tooltip.riposte.slot.parry").formatted(Formatting.GRAY));

            // Loops through our attributes and colors them Blue
            for (String key : tooltipKeys) {
                tooltip.add(Text.translatable(key).formatted(Formatting.BLUE));
            }
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}