package atom.dynamicwood.block;

import atom.dynamicwood.DynamicWoodMod;
import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import atom.dynamicwood.component.ModDataComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class DynamicCraftingTableBlock extends CraftingTableBlock implements EntityBlock {
    public static final MapCodec<DynamicCraftingTableBlock> CODEC = simpleCodec(DynamicCraftingTableBlock::new);
    private static final Component CONTAINER_TITLE = Component.translatable("container.crafting");

    public DynamicCraftingTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CraftingTableBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DynamicWoodBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu(state.getMenuProvider(level, pos));
            player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (syncId, playerInventory, player) -> new DynamicCraftingMenu(syncId, playerInventory, ContainerLevelAccess.create(level, pos)),
                CONTAINER_TITLE
        );
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
        ItemStack stack = new ItemStack(this);
        if (be instanceof DynamicWoodBlockEntity dynamicBe) {
            stack.set(ModDataComponents.WOOD_TYPE, dynamicBe.getWoodType());
        }
        return List.of(stack);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        if (level.getBlockEntity(pos) instanceof DynamicWoodBlockEntity be) {
            stack.set(ModDataComponents.WOOD_TYPE, be.getWoodType());
        }
        return stack;
    }

    public static class DynamicCraftingMenu extends CraftingMenu {
        private final ContainerLevelAccess access;

        public DynamicCraftingMenu(int syncId, Inventory playerInventory, ContainerLevelAccess access) {
            super(syncId, playerInventory, access);
            this.access = access;
        }

        @Override
        public boolean stillValid(Player player) {
            return stillValid(this.access, player, DynamicWoodMod.CRAFTING_TABLE)
                    || stillValid(this.access, player, Blocks.CRAFTING_TABLE);
        }
    }
}
