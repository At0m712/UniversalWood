package atom.dynamicwood.block;

import atom.dynamicwood.DynamicWoodMod;
import atom.dynamicwood.block.entity.DynamicBeehiveBlockEntity;
import atom.dynamicwood.block.properties.DynamicWoodProperties;
import atom.dynamicwood.component.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class DynamicBeehiveBlock extends BeehiveBlock {
    public DynamicBeehiveBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(HONEY_LEVEL, 0)
                .setValue(DynamicWoodProperties.FIREPROOF, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
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
        return new DynamicBeehiveBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, DynamicWoodMod.BEEHIVE_BLOCK_ENTITY_TYPE, BeehiveBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        Identifier woodId = stack.get(ModDataComponents.WOOD_TYPE);
        if (woodId != null && level.getBlockEntity(pos) instanceof DynamicBeehiveBlockEntity be) {
            be.setWoodType(woodId);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // Trigger vanilla BeehiveBlock emergency bee release for TNT/explosions
        super.getDrops(state, params);

        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        Identifier woodId = null;
        if (be instanceof DynamicBeehiveBlockEntity dynamicBe) {
            woodId = dynamicBe.getWoodType();
        }

        ItemStack stack;
        if (DynamicWoodMod.isOak(woodId)) {
            stack = new ItemStack(Items.BEEHIVE);
        } else {
            stack = new ItemStack(this);
            stack.set(ModDataComponents.WOOD_TYPE, woodId);
        }

        var tool = params.getOptionalParameter(LootContextParams.TOOL);
        boolean hasSilkTouch = false;
        if (tool != null) {
            ItemEnchantments enchs = tool.get(DataComponents.ENCHANTMENTS);
            if (enchs != null) {
                hasSilkTouch = enchs.keySet().stream().anyMatch(h -> h.is(Enchantments.SILK_TOUCH));
            }
        }

        if (hasSilkTouch) {
            if (be != null) {
                DataComponentMap components = be.collectComponents();
                Bees bees = components.get(DataComponents.BEES);
                if (bees != null) {
                    stack.set(DataComponents.BEES, bees);
                }
            }
            int honeyLevel = state.getValue(HONEY_LEVEL);
            if (honeyLevel > 0) {
                stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(HONEY_LEVEL, honeyLevel));
            }
        }

        return List.of(stack);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        Identifier woodId = null;
        if (level.getBlockEntity(pos) instanceof DynamicBeehiveBlockEntity be) {
            woodId = be.getWoodType();
        }
        if (DynamicWoodMod.isOak(woodId)) {
            return new ItemStack(Items.BEEHIVE);
        }
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        stack.set(ModDataComponents.WOOD_TYPE, woodId);
        return stack;
    }
}
