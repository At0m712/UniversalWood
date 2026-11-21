package atom.dynamicwood.block;

import atom.dynamicwood.DynamicWoodMod;
import atom.dynamicwood.block.entity.DynamicChestBlockEntity;
import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import atom.dynamicwood.block.properties.DynamicWoodProperties;
import atom.dynamicwood.component.ModDataComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class DynamicChestBlock extends ChestBlock {
    public static final MapCodec<DynamicChestBlock> CODEC = simpleCodec(DynamicChestBlock::new);

    public DynamicChestBlock(BlockBehaviour.Properties properties) {
        super(() -> DynamicWoodMod.CHEST_BLOCK_ENTITY_TYPE, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(TYPE, ChestType.SINGLE)
                .setValue(WATERLOGGED, false)
                .setValue(DynamicWoodProperties.FIREPROOF, false));
    }

    @Override
    public MapCodec<? extends ChestBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DynamicWoodProperties.FIREPROOF);
    }

    private @Nullable Direction candidateSameWoodPartnerFacing(Level level, BlockPos pos, Direction offsetDir, Identifier woodId) {
        BlockPos neighborPos = pos.relative(offsetDir);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (chestCanConnectTo(neighborState) && neighborState.getValue(TYPE) == ChestType.SINGLE) {
            if (level.getBlockEntity(neighborPos) instanceof DynamicChestBlockEntity neighborBe) {
                if (woodId.equals(neighborBe.getWoodType())) {
                    return neighborState.getValue(FACING);
                }
            }
        }
        return null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = level.getFluidState(pos);
        Direction facing = context.getHorizontalDirection().getOpposite();
        Identifier woodId = context.getItemInHand().get(ModDataComponents.WOOD_TYPE);
        if (woodId == null) {
            woodId = DynamicWoodBlockEntity.DEFAULT_WOOD;
        }

        ChestType chestType = ChestType.SINGLE;
        boolean secondary = context.isSecondaryUseActive();
        Direction clickedFace = context.getClickedFace();

        if (clickedFace.getAxis().isHorizontal() && secondary) {
            Direction partnerFacing = candidateSameWoodPartnerFacing(level, pos, clickedFace.getOpposite(), woodId);
            if (partnerFacing != null && partnerFacing.getAxis() != clickedFace.getAxis()) {
                facing = partnerFacing;
                chestType = partnerFacing.getCounterClockWise() == clickedFace.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
            }
        }

        if (chestType == ChestType.SINGLE && !secondary) {
            if (facing == candidateSameWoodPartnerFacing(level, pos, facing.getClockWise(), woodId)) {
                chestType = ChestType.LEFT;
            } else if (facing == candidateSameWoodPartnerFacing(level, pos, facing.getCounterClockWise(), woodId)) {
                chestType = ChestType.RIGHT;
            }
        }

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(TYPE, chestType)
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER)
                .setValue(DynamicWoodProperties.FIREPROOF, DynamicWoodMod.isFireproofWood(woodId));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        if (chestCanConnectTo(neighborState) && direction.getAxis().isHorizontal()) {
            ChestType neighborType = neighborState.getValue(TYPE);
            if (state.getValue(TYPE) == ChestType.SINGLE
                    && neighborType != ChestType.SINGLE
                    && state.getValue(FACING) == neighborState.getValue(FACING)
                    && getConnectedDirection(neighborState) == direction.getOpposite()) {
                BlockEntity selfBe = level.getBlockEntity(pos);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (selfBe instanceof DynamicChestBlockEntity selfChest && neighborBe instanceof DynamicChestBlockEntity neighborChest) {
                    if (!selfChest.getWoodType().equals(neighborChest.getWoodType())) {
                        return state;
                    }
                }
                return state.setValue(TYPE, neighborType.getOpposite());
            }
        } else if (getConnectedDirection(state) == direction) {
            return state.setValue(TYPE, ChestType.SINGLE);
        }

        return state;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DynamicChestBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, DynamicWoodMod.CHEST_BLOCK_ENTITY_TYPE, ChestBlockEntity::lidAnimateTick) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Identifier woodId = stack.get(ModDataComponents.WOOD_TYPE);
        if (woodId != null && level.getBlockEntity(pos) instanceof DynamicChestBlockEntity be) {
            be.setWoodType(woodId);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        Identifier woodId = null;
        if (be instanceof DynamicChestBlockEntity dynamicBe) {
            woodId = dynamicBe.getWoodType();
        }
        if (DynamicWoodMod.isOak(woodId)) {
            return List.of(new ItemStack(Items.CHEST));
        }
        ItemStack stack = new ItemStack(this);
        stack.set(ModDataComponents.WOOD_TYPE, woodId);
        return List.of(stack);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        Identifier woodId = null;
        if (level.getBlockEntity(pos) instanceof DynamicChestBlockEntity be) {
            woodId = be.getWoodType();
        }
        if (DynamicWoodMod.isOak(woodId)) {
            return new ItemStack(Items.CHEST);
        }
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        stack.set(ModDataComponents.WOOD_TYPE, woodId);
        return stack;
    }
}
