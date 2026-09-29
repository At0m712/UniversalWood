package atom.dynamicwood.block;

import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import atom.dynamicwood.component.ModDataComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class DynamicBookshelfBlock extends Block implements EntityBlock {
    public static final MapCodec<DynamicBookshelfBlock> CODEC = simpleCodec(DynamicBookshelfBlock::new);

    public DynamicBookshelfBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends DynamicBookshelfBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DynamicWoodBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Identifier woodId = stack.get(ModDataComponents.WOOD_TYPE);
        if (woodId != null && level.getBlockEntity(pos) instanceof DynamicWoodBlockEntity be) {
            be.setWoodType(woodId);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var tool = params.getOptionalParameter(LootContextParams.TOOL);
        boolean hasSilkTouch = false;
        if (tool != null) {
            ItemEnchantments enchs = tool.get(DataComponents.ENCHANTMENTS);
            if (enchs != null) {
                hasSilkTouch = enchs.keySet().stream().anyMatch(h -> h.is(Enchantments.SILK_TOUCH));
            }
        }
        if (hasSilkTouch) {
            BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            ItemStack stack = new ItemStack(this);
            if (be instanceof DynamicWoodBlockEntity dynamicBe) {
                stack.set(ModDataComponents.WOOD_TYPE, dynamicBe.getWoodType());
            }
            return List.of(stack);
        }

        return List.of(new ItemStack(Items.BOOK, 3));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        if (level.getBlockEntity(pos) instanceof DynamicWoodBlockEntity be) {
            stack.set(ModDataComponents.WOOD_TYPE, be.getWoodType());
        }
        return stack;
    }
}
