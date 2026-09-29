package atom.dynamicwood;

import atom.dynamicwood.block.DynamicBookshelfBlock;
import atom.dynamicwood.block.DynamicCraftingTableBlock;
import atom.dynamicwood.block.DynamicLadderBlock;
import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import atom.dynamicwood.component.ModDataComponents;
import atom.dynamicwood.item.DynamicWoodBlockItem;
import atom.dynamicwood.item.DynamicWoodItemGroups;
import atom.dynamicwood.recipe.DynamicWoodCraftingRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicWoodMod implements ModInitializer {
    public static final String MOD_ID = "dynamicwood";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ResourceKey<Block> CRAFTING_TABLE_BLOCK_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(MOD_ID, "crafting_table")
    );
    public static final ResourceKey<Item> CRAFTING_TABLE_ITEM_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "crafting_table")
    );

    public static final ResourceKey<Block> BOOKSHELF_BLOCK_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(MOD_ID, "bookshelf")
    );
    public static final ResourceKey<Item> BOOKSHELF_ITEM_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "bookshelf")
    );

    public static final ResourceKey<Block> LADDER_BLOCK_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(MOD_ID, "ladder")
    );
    public static final ResourceKey<Item> LADDER_ITEM_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "ladder")
    );

    public static final DynamicCraftingTableBlock CRAFTING_TABLE = Registry.register(
            BuiltInRegistries.BLOCK,
            CRAFTING_TABLE_BLOCK_KEY,
            new DynamicCraftingTableBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)
                            .setId(CRAFTING_TABLE_BLOCK_KEY)
            )
    );

    public static final DynamicBookshelfBlock BOOKSHELF = Registry.register(
            BuiltInRegistries.BLOCK,
            BOOKSHELF_BLOCK_KEY,
            new DynamicBookshelfBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF)
                            .setId(BOOKSHELF_BLOCK_KEY)
            )
    );

    public static final DynamicLadderBlock LADDER = Registry.register(
            BuiltInRegistries.BLOCK,
            LADDER_BLOCK_KEY,
            new DynamicLadderBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.LADDER)
                            .setId(LADDER_BLOCK_KEY)
            )
    );

    public static final DynamicWoodBlockItem CRAFTING_TABLE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            CRAFTING_TABLE_ITEM_KEY,
            new DynamicWoodBlockItem(
                    CRAFTING_TABLE,
                    new Item.Properties().setId(CRAFTING_TABLE_ITEM_KEY)
            )
    );

    public static final DynamicWoodBlockItem BOOKSHELF_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            BOOKSHELF_ITEM_KEY,
            new DynamicWoodBlockItem(
                    BOOKSHELF,
                    new Item.Properties().setId(BOOKSHELF_ITEM_KEY)
            )
    );

    public static final DynamicWoodBlockItem LADDER_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            LADDER_ITEM_KEY,
            new DynamicWoodBlockItem(
                    LADDER,
                    new Item.Properties().setId(LADDER_ITEM_KEY)
            )
    );

    public static final BlockEntityType<DynamicWoodBlockEntity> WOOD_BLOCK_ENTITY_TYPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "dynamic_wood"),
            FabricBlockEntityTypeBuilder.create(DynamicWoodBlockEntity::new, CRAFTING_TABLE, BOOKSHELF, LADDER).build()
    );

    @Override
    public void onInitialize() {
        LOGGER.info("[DynamicWoodFurniture] Initializing common components and registrations...");
        ModDataComponents.initialize();
        DynamicWoodItemGroups.initialize();

        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(MOD_ID, "dynamic_crafting"),
                DynamicWoodCraftingRecipe.SERIALIZER
        );

        LOGGER.info("[DynamicWoodFurniture] Successfully initialized!");
    }
}
