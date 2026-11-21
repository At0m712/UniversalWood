package atom.dynamicwood.client;

import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.resources.Identifier;

public class DynamicChestRenderState extends ChestRenderState {
    public Identifier woodType = DynamicWoodBlockEntity.DEFAULT_WOOD;
}
