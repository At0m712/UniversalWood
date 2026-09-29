package atom.dynamicwood.block.entity;

import atom.dynamicwood.DynamicWoodMod;
import atom.dynamicwood.component.ModDataComponents;
import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DynamicWoodBlockEntity extends BlockEntity implements RenderDataBlockEntity {
    public static final Identifier DEFAULT_WOOD = Identifier.fromNamespaceAndPath("minecraft", "oak_planks");
    private Identifier woodType = DEFAULT_WOOD;

    public DynamicWoodBlockEntity(BlockPos pos, BlockState state) {
        super(DynamicWoodMod.WOOD_BLOCK_ENTITY_TYPE, pos, state);
    }

    public Identifier getWoodType() {
        return woodType != null ? woodType : DEFAULT_WOOD;
    }

    public void setWoodType(Identifier woodType) {
        this.woodType = (woodType != null) ? woodType : DEFAULT_WOOD;
        setChanged();
        if (level != null) {
            if (!level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            } else {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("wood_type", Identifier.CODEC, getWoodType());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.woodType = input.read("wood_type", Identifier.CODEC).orElse(DEFAULT_WOOD);
        if (level != null && level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Identifier wood = components.get(ModDataComponents.WOOD_TYPE);
        if (wood != null) {
            this.woodType = wood;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(ModDataComponents.WOOD_TYPE, getWoodType());
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("wood_type");
    }

    @Override
    public Object getRenderData() {
        return getWoodType();
    }
}
