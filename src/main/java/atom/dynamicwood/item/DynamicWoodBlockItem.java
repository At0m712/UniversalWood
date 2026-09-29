package atom.dynamicwood.item;

import atom.dynamicwood.component.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class DynamicWoodBlockItem extends BlockItem {
    public DynamicWoodBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier woodId = stack.get(ModDataComponents.WOOD_TYPE);
        if (woodId != null) {
            Item plankItem = BuiltInRegistries.ITEM.getOptional(woodId).orElse(null);
            if (plankItem != null) {
                return Component.translatable(this.getDescriptionId() + ".typed", plankItem.getName(new ItemStack(plankItem)));
            }
        }
        return super.getName(stack);
    }
}
