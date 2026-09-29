package atom.dynamicwood.client;

import atom.dynamicwood.DynamicWoodMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class DynamicWoodClient implements ClientModInitializer {
    public static final DynamicWoodModel CRAFTING_TABLE_MODEL = new DynamicWoodModel(DynamicWoodModel.FurnitureType.CRAFTING_TABLE);
    public static final DynamicWoodModel BOOKSHELF_MODEL = new DynamicWoodModel(DynamicWoodModel.FurnitureType.BOOKSHELF);

    public static final Map<Direction, DynamicWoodModel> LADDER_BLOCK_MODELS = Map.of(
            Direction.NORTH, new DynamicWoodModel(DynamicWoodModel.FurnitureType.LADDER, Direction.NORTH),
            Direction.EAST, new DynamicWoodModel(DynamicWoodModel.FurnitureType.LADDER, Direction.EAST),
            Direction.SOUTH, new DynamicWoodModel(DynamicWoodModel.FurnitureType.LADDER, Direction.SOUTH),
            Direction.WEST, new DynamicWoodModel(DynamicWoodModel.FurnitureType.LADDER, Direction.WEST)
    );
    public static final DynamicWoodModel LADDER_ITEM_MODEL = new DynamicWoodModel(DynamicWoodModel.FurnitureType.LADDER, Direction.NORTH);

    @Override
    public void onInitializeClient() {
        DynamicWoodMod.LOGGER.info("[DynamicWoodFurniture] Initializing client renderer and model loading plugins...");

        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.modifyBlockModelAfterBake().register((model, context) -> {
                BlockState state = context.state();
                if (state != null) {
                    if (state.is(DynamicWoodMod.CRAFTING_TABLE)) {
                        return CRAFTING_TABLE_MODEL;
                    }
                    if (state.is(DynamicWoodMod.BOOKSHELF)) {
                        return BOOKSHELF_MODEL;
                    }
                    if (state.is(DynamicWoodMod.LADDER)) {
                        Direction facing = state.hasProperty(LadderBlock.FACING) ? state.getValue(LadderBlock.FACING) : Direction.NORTH;
                        return LADDER_BLOCK_MODELS.getOrDefault(facing, LADDER_ITEM_MODEL);
                    }
                }
                return model;
            });

            pluginContext.modifyItemModelAfterBake().register((model, context) -> {
                Identifier id = context.itemId();
                if (id != null) {
                    if (id.equals(Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "crafting_table"))) {
                        CRAFTING_TABLE_MODEL.setItemPropertiesFrom(model);
                        return CRAFTING_TABLE_MODEL;
                    }
                    if (id.equals(Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "bookshelf"))) {
                        BOOKSHELF_MODEL.setItemPropertiesFrom(model);
                        return BOOKSHELF_MODEL;
                    }
                    if (id.equals(Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "ladder"))) {
                        LADDER_ITEM_MODEL.setItemPropertiesFrom(model);
                        for (DynamicWoodModel m : LADDER_BLOCK_MODELS.values()) {
                            m.setItemPropertiesFrom(model);
                        }
                        return LADDER_ITEM_MODEL;
                    }
                }
                return model;
            });
        });

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new DynamicWoodReloadListener());

        DynamicWoodMod.LOGGER.info("[DynamicWoodFurniture] Client initialization complete!");
    }
}
