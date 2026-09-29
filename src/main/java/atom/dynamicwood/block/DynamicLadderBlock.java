package atom.dynamicwood.block;

import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import atom.dynamicwood.component.ModDataComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class DynamicLadderBlock extends LadderBlock implements EntityBlock {
    public static final MapCodec<DynamicLadderBlock> CODEC = simpleCodec(DynamicLadderBlock::new);

    public DynamicLadderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private static boolean isOak(@Nullable Identifier woodId) {
        if (woodId == null) {
            return true;
        }
        if (woodId.equals(DynamicWoodBlockEntity.DEFAULT_WOOD)) {
            return true;
        }
        String path = woodId.getPath();
        if (path.startsWith("block/")) {
            path = path.substring(6);
        }
        return woodId.getNamespace().equals("minecraft") && path.equals("oak_planks");
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<LadderBlock> codec() {
        return (MapCodec<LadderBlock>) (MapCodec<?>) CODEC;
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
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        Identifier woodId = null;
        if (be instanceof DynamicWoodBlockEntity dynamicBe) {
            woodId = dynamicBe.getWoodType();
        }
        if (isOak(woodId)) {
            return List.of(new ItemStack(Items.LADDER));
        }
        ItemStack stack = new ItemStack(this);
        stack.set(ModDataComponents.WOOD_TYPE, woodId);
        return List.of(stack);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        Identifier woodId = null;
        if (level.getBlockEntity(pos) instanceof DynamicWoodBlockEntity be) {
            woodId = be.getWoodType();
        }
        if (isOak(woodId)) {
            return new ItemStack(Items.LADDER);
        }
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        stack.set(ModDataComponents.WOOD_TYPE, woodId);
        return stack;
    }
}
