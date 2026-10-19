package atom.dynamicwood.block;

import atom.dynamicwood.DynamicWoodMod;
import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import atom.dynamicwood.block.properties.DynamicWoodProperties;
import atom.dynamicwood.component.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class DynamicLadderBlock extends LadderBlock implements EntityBlock {

    public DynamicLadderBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false).setValue(DynamicWoodProperties.FIREPROOF, false));
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
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DynamicWoodProperties.FIREPROOF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null) {
            Identifier woodId = context.getItemInHand().get(ModDataComponents.WOOD_TYPE);
            state = state.setValue(DynamicWoodProperties.FIREPROOF, DynamicWoodMod.isFireproofWood(woodId));
        }
        return state;
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
