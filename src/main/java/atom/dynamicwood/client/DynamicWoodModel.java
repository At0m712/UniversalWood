package atom.dynamicwood.client;

import atom.dynamicwood.DynamicWoodMod;
import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import atom.dynamicwood.component.ModDataComponents;
import com.google.common.base.Suppliers;
import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModelPart;
import net.fabricmc.fabric.impl.client.renderer.VanillaBlockModelPartEncoder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DynamicWoodModel implements BlockStateModel, FabricBlockStateModel, ItemModel {
    public enum FurnitureType {
        CRAFTING_TABLE,
        BOOKSHELF,
        LADDER
    }

    public record ModelCacheKey(Identifier woodId, Direction facing) {
    }

    public record CachedModelData(
            @Nullable Mesh mesh,
            List<BakedQuad> itemQuads,
            BlockStateModelPart part,
            Material.Baked particleMaterial,
            Supplier<Vector3fc[]> extents
    ) {
    }

    private static final List<DynamicWoodModel> REGISTERED_MODELS = new CopyOnWriteArrayList<>();
    private static final float EPSILON = 0.008f;

    private static final ItemTransform DEFAULT_GUI_TRANSFORM = new ItemTransform(
            new Vector3f(30, 225, 0),
            new Vector3f(0, 0, 0),
            new Vector3f(0.625f, 0.625f, 0.625f)
    );
    private static final ItemTransform DEFAULT_GROUND_TRANSFORM = new ItemTransform(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 3f / 16f, 0),
            new Vector3f(0.25f, 0.25f, 0.25f)
    );
    private static final ItemTransform DEFAULT_FIXED_TRANSFORM = new ItemTransform(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, 0),
            new Vector3f(0.5f, 0.5f, 0.5f)
    );
    private static final ItemTransform DEFAULT_THIRD_PERSON_TRANSFORM = new ItemTransform(
            new Vector3f(75, 45, 0),
            new Vector3f(0, 2.5f / 16f, 0),
            new Vector3f(0.375f, 0.375f, 0.375f)
    );
    private static final ItemTransform DEFAULT_FIRST_PERSON_TRANSFORM = new ItemTransform(
            new Vector3f(0, 45, 0),
            new Vector3f(0, 0, 0),
            new Vector3f(0.4f, 0.4f, 0.4f)
    );

    private static final ItemTransform DEFAULT_LADDER_GUI_TRANSFORM = new ItemTransform(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 0, 0),
            new Vector3f(1.0f, 1.0f, 1.0f)
    );
    private static final ItemTransform DEFAULT_LADDER_GROUND_TRANSFORM = new ItemTransform(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 2f / 16f, 0),
            new Vector3f(0.5f, 0.5f, 0.5f)
    );
    private static final ItemTransform DEFAULT_LADDER_FIXED_TRANSFORM = new ItemTransform(
            new Vector3f(0, 180, 0),
            new Vector3f(0, 0, 0),
            new Vector3f(1.0f, 1.0f, 1.0f)
    );
    private static final ItemTransform DEFAULT_LADDER_THIRD_PERSON_TRANSFORM = new ItemTransform(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 3f / 16f, 1f / 16f),
            new Vector3f(0.55f, 0.55f, 0.55f)
    );
    private static final ItemTransform DEFAULT_LADDER_FIRST_PERSON_TRANSFORM = new ItemTransform(
            new Vector3f(0, -90, 25),
            new Vector3f(1.13f / 16f, 3.2f / 16f, 1.13f / 16f),
            new Vector3f(0.68f, 0.68f, 0.68f)
    );

    private final FurnitureType furnitureType;
    private final @Nullable Direction blockFacing;
    private final ConcurrentHashMap<ModelCacheKey, CachedModelData> cache = new ConcurrentHashMap<>();
    private @Nullable ModelRenderProperties itemRenderProperties;
    private @Nullable Matrix4fc itemTransformation;

    public DynamicWoodModel(FurnitureType furnitureType) {
        this(furnitureType, null);
    }

    public DynamicWoodModel(FurnitureType furnitureType, @Nullable Direction blockFacing) {
        this.furnitureType = furnitureType;
        this.blockFacing = blockFacing;
        REGISTERED_MODELS.add(this);
    }

    public static void clearAllCaches() {
        for (DynamicWoodModel model : REGISTERED_MODELS) {
            model.cache.clear();
        }
    }

    public void setItemPropertiesFrom(ItemModel model) {
        if (model instanceof CuboidItemModelWrapper) {
            try {
                Field f = CuboidItemModelWrapper.class.getDeclaredField("properties");
                f.setAccessible(true);
                this.itemRenderProperties = (ModelRenderProperties) f.get(model);
            } catch (Exception e) {
                DynamicWoodMod.LOGGER.debug("[DynamicWoodFurniture] Failed to extract item properties from model", e);
            }
            try {
                Field fTransform = CuboidItemModelWrapper.class.getDeclaredField("transformation");
                fTransform.setAccessible(true);
                this.itemTransformation = (Matrix4fc) fTransform.get(model);
            } catch (Exception e) {
                DynamicWoodMod.LOGGER.debug("[DynamicWoodFurniture] Failed to extract item transformation from model", e);
            }
        }
    }

    private boolean isOak(Identifier woodId) {
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

    public CachedModelData getOrBuildModelData(ModelCacheKey key) {
        Identifier safeWoodId = (key.woodId() != null) ? key.woodId() : DynamicWoodBlockEntity.DEFAULT_WOOD;
        Direction safeFacing = (key.facing() != null) ? key.facing() : Direction.NORTH;
        ModelCacheKey safeKey = new ModelCacheKey(safeWoodId, safeFacing);
        return cache.computeIfAbsent(safeKey, this::buildModelData);
    }

    private CachedModelData buildModelData(ModelCacheKey key) {
        if (furnitureType == FurnitureType.LADDER) {
            return buildLadderModelData(key.woodId(), key.facing());
        } else {
            return buildCubeModelData(key.woodId());
        }
    }

    private ModelState getLadderRotation(Direction facing) {
        if (facing == null) {
            return BlockModelRotation.IDENTITY;
        }
        return switch (facing) {
            case EAST -> BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_Y_90);
            case SOUTH -> BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_Y_180);
            case WEST -> BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_Y_270);
            default -> BlockModelRotation.IDENTITY;
        };
    }

    private CachedModelData buildLadderModelData(Identifier woodId, Direction facing) {
        ModelBaker.Interner interner = new ModelBaker.Interner() {
            @Override
            public Vector3fc vector(Vector3fc v) {
                return v;
            }

            @Override
            public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo m) {
                return m;
            }
        };

        ModelState modelState = getLadderRotation(facing);
        Map<Direction, List<BakedQuad>> culledQuads = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            culledQuads.put(dir, new ArrayList<>());
        }
        List<BakedQuad> unculledQuads = new ArrayList<>();
        List<BakedQuad> allQuads = new ArrayList<>();

        if (isOak(woodId)) {
            TextureAtlas atlas = getBlockAtlas();
            TextureAtlasSprite ladderSprite = (atlas != null)
                    ? atlas.getSprite(Identifier.fromNamespaceAndPath("minecraft", "block/ladder"))
                    : null;
            if (!isValidSprite(ladderSprite)) {
                ladderSprite = resolvePlankSprite(DynamicWoodBlockEntity.DEFAULT_WOOD);
            }
            Material.Baked particleMat = new Material.Baked(ladderSprite, false);

            BakedQuad.MaterialInfo matInfo = new BakedQuad.MaterialInfo(
                    ladderSprite,
                    ChunkSectionLayer.CUTOUT,
                    Sheets.cutoutBlockItemSheet(),
                    -1,
                    true,
                    0
            );

            Vector3f from = new Vector3f(0, 0, 15.2f);
            Vector3f to = new Vector3f(16, 16, 15.2f);
            CuboidFace.UVs northUvs = new CuboidFace.UVs(0, 0, 16, 16);
            CuboidFace.UVs southUvs = new CuboidFace.UVs(16, 0, 0, 16);

            BakedQuad northQuad = FaceBakery.bakeQuad(interner, from, to, northUvs, Quadrant.R0, matInfo, Direction.NORTH, modelState, null);
            BakedQuad southQuad = FaceBakery.bakeQuad(interner, from, to, southUvs, Quadrant.R0, matInfo, Direction.SOUTH, modelState, null);

            unculledQuads.add(northQuad);
            unculledQuads.add(southQuad);
            allQuads.add(northQuad);
            allQuads.add(southQuad);

            List<BakedQuad> itemQuads = buildLadder3DItemQuads(interner, matInfo);
            BlockStateModelPart part = new DynamicBlockStateModelPart(culledQuads, unculledQuads, particleMat);
            Mesh mesh = buildMesh(allQuads);
            Supplier<Vector3fc[]> extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(itemQuads));
            return new CachedModelData(mesh, itemQuads, part, particleMat, extents);
        }

        // For non-oak wood types: build authentic ladder geometry using the wood plank sprite
        TextureAtlasSprite plankSprite = resolvePlankSprite(woodId);
        Material.Baked particleMat = new Material.Baked(plankSprite, false);

        BakedQuad.MaterialInfo matInfo = new BakedQuad.MaterialInfo(
                plankSprite,
                ChunkSectionLayer.CUTOUT,
                Sheets.cutoutBlockItemSheet(),
                -1,
                true,
                0
        );

        // 1. Left Vertical Rail (x in [2, 4], y in [0, 16])
        Vector3f railLeftFrom = new Vector3f(2, 0, 15.2f);
        Vector3f railLeftTo = new Vector3f(4, 16, 15.2f);
        CuboidFace.UVs railLeftNorth = new CuboidFace.UVs(2, 0, 4, 16);
        CuboidFace.UVs railLeftSouth = new CuboidFace.UVs(14, 0, 12, 16);
        BakedQuad rln = FaceBakery.bakeQuad(interner, railLeftFrom, railLeftTo, railLeftNorth, Quadrant.R0, matInfo, Direction.NORTH, modelState, null);
        BakedQuad rls = FaceBakery.bakeQuad(interner, railLeftFrom, railLeftTo, railLeftSouth, Quadrant.R0, matInfo, Direction.SOUTH, modelState, null);
        unculledQuads.add(rln);
        unculledQuads.add(rls);
        allQuads.add(rln);
        allQuads.add(rls);

        // 2. Right Vertical Rail (x in [12, 14], y in [0, 16])
        Vector3f railRightFrom = new Vector3f(12, 0, 15.2f);
        Vector3f railRightTo = new Vector3f(14, 16, 15.2f);
        CuboidFace.UVs railRightNorth = new CuboidFace.UVs(12, 0, 14, 16);
        CuboidFace.UVs railRightSouth = new CuboidFace.UVs(4, 0, 2, 16);
        BakedQuad rrn = FaceBakery.bakeQuad(interner, railRightFrom, railRightTo, railRightNorth, Quadrant.R0, matInfo, Direction.NORTH, modelState, null);
        BakedQuad rrs = FaceBakery.bakeQuad(interner, railRightFrom, railRightTo, railRightSouth, Quadrant.R0, matInfo, Direction.SOUTH, modelState, null);
        unculledQuads.add(rrn);
        unculledQuads.add(rrs);
        allQuads.add(rrn);
        allQuads.add(rrs);

        // 3. Four Horizontal Rungs: perfectly partitioned to avoid polygon overlaps & z-fighting
        int[][] rungs = {
                {13, 15, 1, 3},   // Rung 0: y in [13, 15], v in [1, 3]
                {9, 11, 5, 7},    // Rung 1: y in [9, 11], v in [5, 7]
                {5, 7, 9, 11},    // Rung 2: y in [5, 7], v in [9, 11]
                {1, 3, 13, 15}    // Rung 3: y in [1, 3], v in [13, 15]
        };

        for (int[] r : rungs) {
            float y0 = r[0];
            float y1 = r[1];
            float v0 = r[2];
            float v1 = r[3];

            // 3a. Left outer tab (x in [1, 2])
            Vector3f tabLFrom = new Vector3f(1, y0, 15.2f);
            Vector3f tabLTo = new Vector3f(2, y1, 15.2f);
            BakedQuad tln = FaceBakery.bakeQuad(interner, tabLFrom, tabLTo, new CuboidFace.UVs(1, v0, 2, v1), Quadrant.R0, matInfo, Direction.NORTH, modelState, null);
            BakedQuad tls = FaceBakery.bakeQuad(interner, tabLFrom, tabLTo, new CuboidFace.UVs(15, v0, 14, v1), Quadrant.R0, matInfo, Direction.SOUTH, modelState, null);
            unculledQuads.add(tln);
            unculledQuads.add(tls);
            allQuads.add(tln);
            allQuads.add(tls);

            // 3b. Center rung bar (x in [4, 12])
            Vector3f barFrom = new Vector3f(4, y0, 15.2f);
            Vector3f barTo = new Vector3f(12, y1, 15.2f);
            BakedQuad bn = FaceBakery.bakeQuad(interner, barFrom, barTo, new CuboidFace.UVs(4, v0, 12, v1), Quadrant.R0, matInfo, Direction.NORTH, modelState, null);
            BakedQuad bs = FaceBakery.bakeQuad(interner, barFrom, barTo, new CuboidFace.UVs(12, v0, 4, v1), Quadrant.R0, matInfo, Direction.SOUTH, modelState, null);
            unculledQuads.add(bn);
            unculledQuads.add(bs);
            allQuads.add(bn);
            allQuads.add(bs);

            // 3c. Right outer tab (x in [14, 15])
            Vector3f tabRFrom = new Vector3f(14, y0, 15.2f);
            Vector3f tabRTo = new Vector3f(15, y1, 15.2f);
            BakedQuad trn = FaceBakery.bakeQuad(interner, tabRFrom, tabRTo, new CuboidFace.UVs(14, v0, 15, v1), Quadrant.R0, matInfo, Direction.NORTH, modelState, null);
            BakedQuad trs = FaceBakery.bakeQuad(interner, tabRFrom, tabRTo, new CuboidFace.UVs(2, v0, 1, v1), Quadrant.R0, matInfo, Direction.SOUTH, modelState, null);
            unculledQuads.add(trn);
            unculledQuads.add(trs);
            allQuads.add(trn);
            allQuads.add(trs);
        }

        List<BakedQuad> itemQuads = buildLadder3DItemQuads(interner, matInfo);
        BlockStateModelPart part = new DynamicBlockStateModelPart(culledQuads, unculledQuads, particleMat);
        Mesh mesh = buildMesh(allQuads);
        Supplier<Vector3fc[]> extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(itemQuads));
        return new CachedModelData(mesh, itemQuads, part, particleMat, extents);
    }

    private List<BakedQuad> buildLadder3DItemQuads(ModelBaker.Interner interner, BakedQuad.MaterialInfo matInfo) {
        List<BakedQuad> itemQuads = new ArrayList<>();
        float z0 = 7.5f;
        float z1 = 8.5f;
        ModelState itemRot = BlockModelRotation.IDENTITY;

        // 1. Left Rail (x: 2..4, y: 0..16)
        Vector3f lrFrom = new Vector3f(2, 0, z0);
        Vector3f lrTo = new Vector3f(4, 16, z1);
        itemQuads.add(FaceBakery.bakeQuad(interner, lrFrom, lrTo, new CuboidFace.UVs(2, 0, 4, 16), Quadrant.R0, matInfo, Direction.SOUTH, itemRot, null));
        itemQuads.add(FaceBakery.bakeQuad(interner, lrFrom, lrTo, new CuboidFace.UVs(14, 0, 12, 16), Quadrant.R0, matInfo, Direction.NORTH, itemRot, null));
        itemQuads.add(FaceBakery.bakeQuad(interner, lrFrom, lrTo, new CuboidFace.UVs(2, 0, 4, 1), Quadrant.R0, matInfo, Direction.UP, itemRot, null));
        itemQuads.add(FaceBakery.bakeQuad(interner, lrFrom, lrTo, new CuboidFace.UVs(2, 15, 4, 16), Quadrant.R0, matInfo, Direction.DOWN, itemRot, null));
        itemQuads.add(FaceBakery.bakeQuad(interner, lrFrom, lrTo, new CuboidFace.UVs(2, 0, 3, 16), Quadrant.R0, matInfo, Direction.WEST, itemRot, null));

        // Inner East edges of Left Rail between rungs
        float[][] lrGaps = {
                {0, 1, 15, 16},
                {3, 5, 11, 13},
                {7, 9, 7, 9},
                {11, 13, 3, 5},
                {15, 16, 0, 1}
        };
        for (float[] g : lrGaps) {
            Vector3f gFrom = new Vector3f(2, g[0], z0);
            Vector3f gTo = new Vector3f(4, g[1], z1);
            itemQuads.add(FaceBakery.bakeQuad(interner, gFrom, gTo, new CuboidFace.UVs(3, g[2], 4, g[3]), Quadrant.R0, matInfo, Direction.EAST, itemRot, null));
        }

        // 2. Right Rail (x: 12..14, y: 0..16)
        Vector3f rrFrom = new Vector3f(12, 0, z0);
        Vector3f rrTo = new Vector3f(14, 16, z1);
        itemQuads.add(FaceBakery.bakeQuad(interner, rrFrom, rrTo, new CuboidFace.UVs(12, 0, 14, 16), Quadrant.R0, matInfo, Direction.SOUTH, itemRot, null));
        itemQuads.add(FaceBakery.bakeQuad(interner, rrFrom, rrTo, new CuboidFace.UVs(4, 0, 2, 16), Quadrant.R0, matInfo, Direction.NORTH, itemRot, null));
        itemQuads.add(FaceBakery.bakeQuad(interner, rrFrom, rrTo, new CuboidFace.UVs(12, 0, 14, 1), Quadrant.R0, matInfo, Direction.UP, itemRot, null));
        itemQuads.add(FaceBakery.bakeQuad(interner, rrFrom, rrTo, new CuboidFace.UVs(12, 15, 14, 16), Quadrant.R0, matInfo, Direction.DOWN, itemRot, null));
        itemQuads.add(FaceBakery.bakeQuad(interner, rrFrom, rrTo, new CuboidFace.UVs(13, 0, 14, 16), Quadrant.R0, matInfo, Direction.EAST, itemRot, null));

        // Inner West edges of Right Rail between rungs
        for (float[] g : lrGaps) {
            Vector3f gFrom = new Vector3f(12, g[0], z0);
            Vector3f gTo = new Vector3f(14, g[1], z1);
            itemQuads.add(FaceBakery.bakeQuad(interner, gFrom, gTo, new CuboidFace.UVs(12, g[2], 13, g[3]), Quadrant.R0, matInfo, Direction.WEST, itemRot, null));
        }

        // 3. Four Rungs (y: [13, 15], [9, 11], [5, 7], [1, 3])
        int[][] rungs = {
                {13, 15, 1, 3},
                {9, 11, 5, 7},
                {5, 7, 9, 11},
                {1, 3, 13, 15}
        };

        for (int[] r : rungs) {
            float y0 = r[0];
            float y1 = r[1];
            float v0 = r[2];
            float v1 = r[3];

            // 3a. Left outer tab (x: 1..2)
            Vector3f tLFrom = new Vector3f(1, y0, z0);
            Vector3f tLTo = new Vector3f(2, y1, z1);
            itemQuads.add(FaceBakery.bakeQuad(interner, tLFrom, tLTo, new CuboidFace.UVs(1, v0, 2, v1), Quadrant.R0, matInfo, Direction.SOUTH, itemRot, null));
            itemQuads.add(FaceBakery.bakeQuad(interner, tLFrom, tLTo, new CuboidFace.UVs(15, v0, 14, v1), Quadrant.R0, matInfo, Direction.NORTH, itemRot, null));
            itemQuads.add(FaceBakery.bakeQuad(interner, tLFrom, tLTo, new CuboidFace.UVs(1, v0, 2, v0 + 1), Quadrant.R0, matInfo, Direction.UP, itemRot, null));
            itemQuads.add(FaceBakery.bakeQuad(interner, tLFrom, tLTo, new CuboidFace.UVs(1, v1 - 1, 2, v1), Quadrant.R0, matInfo, Direction.DOWN, itemRot, null));
            itemQuads.add(FaceBakery.bakeQuad(interner, tLFrom, tLTo, new CuboidFace.UVs(1, v0, 2, v1), Quadrant.R0, matInfo, Direction.WEST, itemRot, null));

            // 3b. Center bar (x: 4..12)
            Vector3f cFrom = new Vector3f(4, y0, z0);
            Vector3f cTo = new Vector3f(12, y1, z1);
            itemQuads.add(FaceBakery.bakeQuad(interner, cFrom, cTo, new CuboidFace.UVs(4, v0, 12, v1), Quadrant.R0, matInfo, Direction.SOUTH, itemRot, null));
            itemQuads.add(FaceBakery.bakeQuad(interner, cFrom, cTo, new CuboidFace.UVs(12, v0, 4, v1), Quadrant.R0, matInfo, Direction.NORTH, itemRot, null));
            itemQuads.add(FaceBakery.bakeQuad(interner, cFrom, cTo, new CuboidFace.UVs(4, v0, 12, v0 + 1), Quadrant.R0, matInfo, Direction.UP, itemRot, null));
            itemQuads.add(FaceBakery.bakeQuad(interner, cFrom, cTo, new CuboidFace.UVs(4, v1 - 1, 12, v1), Quadrant.R0, matInfo, Direction.DOWN, itemRot, null));

            // 3c. Right outer tab (x: 14..15)
            Vector3f tRFrom = new Vector3f(14, y0, z0);
            Vector3f tRTo = new Vector3f(15, y1, z1);
            itemQuads.add(FaceBakery.bakeQuad(interner, tRFrom, tRTo, new CuboidFace.UVs(14, v0, 15, v1), Quadrant.R0, matInfo, Direction.SOUTH, itemRot, null));
            itemQuads.add(FaceBakery.bakeQuad(interner, tRFrom, tRTo, new CuboidFace.UVs(2, v0, 1, v1), Quadrant.R0, matInfo, Direction.NORTH, itemRot, null));
            itemQuads.add(FaceBakery.bakeQuad(interner, tRFrom, tRTo, new CuboidFace.UVs(14, v0, 15, v0 + 1), Quadrant.R0, matInfo, Direction.UP, itemRot, null));
            itemQuads.add(FaceBakery.bakeQuad(interner, tRFrom, tRTo, new CuboidFace.UVs(14, v1 - 1, 15, v1), Quadrant.R0, matInfo, Direction.DOWN, itemRot, null));
            itemQuads.add(FaceBakery.bakeQuad(interner, tRFrom, tRTo, new CuboidFace.UVs(14, v0, 15, v1), Quadrant.R0, matInfo, Direction.EAST, itemRot, null));
        }

        return itemQuads;
    }

    private @Nullable Mesh buildMesh(List<BakedQuad> quads) {
        try {
            Renderer renderer = Renderer.get();
            if (renderer != null) {
                MutableMesh mutableMesh = renderer.mutableMesh();
                QuadEmitter emitter = mutableMesh.emitter();
                for (BakedQuad q : quads) {
                    emitter.fromBakedQuad(q);
                    emitter.cullFace(null);
                    emitter.emit();
                }
                return mutableMesh.immutableCopy();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private CachedModelData buildCubeModelData(Identifier woodId) {
        if (isOak(woodId)) {
            return buildOakModelData();
        }

        TextureAtlasSprite plankSprite = resolvePlankSprite(woodId);
        Material.Baked particleMat = new Material.Baked(plankSprite, false);

        Map<Direction, List<BakedQuad>> culledQuads = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            culledQuads.put(dir, new ArrayList<>());
        }
        List<BakedQuad> unculledQuads = new ArrayList<>();
        List<BakedQuad> allQuads = new ArrayList<>();

        ModelBaker.Interner interner = new ModelBaker.Interner() {
            @Override
            public Vector3fc vector(Vector3fc v) {
                return v;
            }

            @Override
            public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo m) {
                return m;
            }
        };

        CuboidFace.UVs fullUvs = new CuboidFace.UVs(0, 0, 16, 16);
        Quadrant rot = Quadrant.R0;
        ModelState modelState = BlockModelRotation.IDENTITY;

        for (Direction dir : Direction.values()) {
            TextureAtlasSprite overlaySprite = resolveOverlaySprite(dir);

            if (overlaySprite == null) {
                Vector3f from = new Vector3f(0, 0, 0);
                Vector3f to = new Vector3f(16, 16, 16);

                BakedQuad.MaterialInfo matInfo = new BakedQuad.MaterialInfo(
                        plankSprite,
                        ChunkSectionLayer.SOLID,
                        Sheets.cutoutBlockItemSheet(),
                        -1,
                        true,
                        0
                );

                BakedQuad quad = FaceBakery.bakeQuad(
                        interner,
                        from,
                        to,
                        fullUvs,
                        rot,
                        matInfo,
                        dir,
                        modelState,
                        null
                );

                culledQuads.get(dir).add(quad);
                allQuads.add(quad);
            } else {
                Vector3f baseFrom = new Vector3f(0, 0, 0);
                Vector3f baseTo = new Vector3f(16, 16, 16);

                switch (dir) {
                    case UP -> baseTo.y = 16.0f - EPSILON;
                    case DOWN -> baseFrom.y = EPSILON;
                    case NORTH -> baseFrom.z = EPSILON;
                    case SOUTH -> baseTo.z = 16.0f - EPSILON;
                    case WEST -> baseFrom.x = EPSILON;
                    case EAST -> baseTo.x = 16.0f - EPSILON;
                }

                // Layer 1: Background Wood Plank (Solid)
                BakedQuad.MaterialInfo baseMatInfo = new BakedQuad.MaterialInfo(
                        plankSprite,
                        ChunkSectionLayer.SOLID,
                        Sheets.cutoutBlockItemSheet(),
                        -1,
                        true,
                        0
                );

                BakedQuad baseQuad = FaceBakery.bakeQuad(
                        interner,
                        baseFrom,
                        baseTo,
                        fullUvs,
                        rot,
                        baseMatInfo,
                        dir,
                        modelState,
                        null
                );

                culledQuads.get(dir).add(baseQuad);
                allQuads.add(baseQuad);

                // Layer 2: Overlay (Cutout)
                Vector3f overlayFrom = new Vector3f(0, 0, 0);
                Vector3f overlayTo = new Vector3f(16, 16, 16);

                BakedQuad.MaterialInfo overlayMatInfo = new BakedQuad.MaterialInfo(
                        overlaySprite,
                        ChunkSectionLayer.CUTOUT,
                        Sheets.cutoutBlockItemSheet(),
                        -1,
                        true,
                        0
                );

                BakedQuad overlayQuad = FaceBakery.bakeQuad(
                        interner,
                        overlayFrom,
                        overlayTo,
                        fullUvs,
                        rot,
                        overlayMatInfo,
                        dir,
                        modelState,
                        null
                );

                culledQuads.get(dir).add(overlayQuad);
                allQuads.add(overlayQuad);
            }
        }

        BlockStateModelPart part = new DynamicBlockStateModelPart(culledQuads, unculledQuads, particleMat);

        Mesh mesh = null;
        try {
            Renderer renderer = Renderer.get();
            if (renderer != null) {
                MutableMesh mutableMesh = renderer.mutableMesh();
                QuadEmitter emitter = mutableMesh.emitter();
                for (BakedQuad q : allQuads) {
                    emitter.fromBakedQuad(q);
                    emitter.cullFace(q.direction());
                    emitter.emit();
                }
                mesh = mutableMesh.immutableCopy();
            }
        } catch (Exception ignored) {
        }

        Supplier<Vector3fc[]> extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(allQuads));
        return new CachedModelData(mesh, allQuads, part, particleMat, extents);
    }

    private CachedModelData buildOakModelData() {
        TextureAtlas atlas = getBlockAtlas();
        TextureAtlasSprite particleSprite = (furnitureType == FurnitureType.CRAFTING_TABLE)
                ? (atlas != null ? atlas.getSprite(Identifier.fromNamespaceAndPath("minecraft", "block/crafting_table_front")) : null)
                : (atlas != null ? atlas.getSprite(Identifier.fromNamespaceAndPath("minecraft", "block/bookshelf")) : null);

        if (!isValidSprite(particleSprite)) {
            particleSprite = resolvePlankSprite(DynamicWoodBlockEntity.DEFAULT_WOOD);
        }
        Material.Baked particleMat = new Material.Baked(particleSprite, false);

        Map<Direction, List<BakedQuad>> culledQuads = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            culledQuads.put(dir, new ArrayList<>());
        }
        List<BakedQuad> unculledQuads = new ArrayList<>();
        List<BakedQuad> allQuads = new ArrayList<>();

        ModelBaker.Interner interner = new ModelBaker.Interner() {
            @Override
            public Vector3fc vector(Vector3fc v) {
                return v;
            }

            @Override
            public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo m) {
                return m;
            }
        };

        CuboidFace.UVs fullUvs = new CuboidFace.UVs(0, 0, 16, 16);
        Quadrant rot = Quadrant.R0;
        ModelState modelState = BlockModelRotation.IDENTITY;

        for (Direction dir : Direction.values()) {
            TextureAtlasSprite faceSprite = resolveOakFaceSprite(dir);
            if (!isValidSprite(faceSprite)) {
                faceSprite = particleSprite;
            }

            Vector3f from = new Vector3f(0, 0, 0);
            Vector3f to = new Vector3f(16, 16, 16);

            BakedQuad.MaterialInfo matInfo = new BakedQuad.MaterialInfo(
                    faceSprite,
                    ChunkSectionLayer.SOLID,
                    Sheets.cutoutBlockItemSheet(),
                    -1,
                    true,
                    0
            );

            BakedQuad quad = FaceBakery.bakeQuad(
                    interner,
                    from,
                    to,
                    fullUvs,
                    rot,
                    matInfo,
                    dir,
                    modelState,
                    null
            );

            culledQuads.get(dir).add(quad);
            allQuads.add(quad);
        }

        BlockStateModelPart part = new DynamicBlockStateModelPart(culledQuads, unculledQuads, particleMat);

        Mesh mesh = null;
        try {
            Renderer renderer = Renderer.get();
            if (renderer != null) {
                MutableMesh mutableMesh = renderer.mutableMesh();
                QuadEmitter emitter = mutableMesh.emitter();
                for (BakedQuad q : allQuads) {
                    emitter.fromBakedQuad(q);
                    emitter.cullFace(q.direction());
                    emitter.emit();
                }
                mesh = mutableMesh.immutableCopy();
            }
        } catch (Exception ignored) {
        }

        Supplier<Vector3fc[]> extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(allQuads));
        return new CachedModelData(mesh, allQuads, part, particleMat, extents);
    }

    private @Nullable TextureAtlasSprite resolveOakFaceSprite(Direction dir) {
        TextureAtlas atlas = getBlockAtlas();
        if (atlas == null) return null;
        if (furnitureType == FurnitureType.CRAFTING_TABLE) {
            return switch (dir) {
                case UP -> resolveAtlasSprite(atlas, Identifier.fromNamespaceAndPath("minecraft", "block/crafting_table_top"), null);
                case NORTH, WEST -> resolveAtlasSprite(atlas, Identifier.fromNamespaceAndPath("minecraft", "block/crafting_table_front"), null);
                case SOUTH, EAST -> resolveAtlasSprite(atlas, Identifier.fromNamespaceAndPath("minecraft", "block/crafting_table_side"), null);
                case DOWN -> resolveAtlasSprite(atlas, Identifier.fromNamespaceAndPath("minecraft", "block/oak_planks"), null);
            };
        } else {
            return switch (dir) {
                case NORTH, SOUTH, EAST, WEST -> resolveAtlasSprite(atlas, Identifier.fromNamespaceAndPath("minecraft", "block/bookshelf"), null);
                case UP, DOWN -> resolveAtlasSprite(atlas, Identifier.fromNamespaceAndPath("minecraft", "block/oak_planks"), null);
            };
        }
    }

    private @Nullable TextureAtlas getBlockAtlas() {
        TextureAtlas atlas = null;
        try {
            atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        } catch (Exception ignored) {
        }

        if (atlas == null) {
            try {
                AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
                if (tex instanceof TextureAtlas ta) {
                    atlas = ta;
                }
            } catch (Exception ignored) {
            }
        }
        return atlas;
    }

    private TextureAtlasSprite resolvePlankSprite(Identifier woodId) {
        if (woodId == null) {
            woodId = DynamicWoodBlockEntity.DEFAULT_WOOD;
        }

        TextureAtlas atlas = getBlockAtlas();

        // 1. Direct path lookup in the block atlas: e.g. "minecraft:block/birch_planks"
        if (atlas != null) {
            String path = woodId.getPath();
            if (!path.startsWith("block/")) {
                path = "block/" + path;
            }
            Identifier spriteId = Identifier.fromNamespaceAndPath(woodId.getNamespace(), path);
            TextureAtlasSprite sprite = atlas.getSprite(spriteId);
            if (isValidSprite(sprite)) {
                return sprite;
            }
        }

        // 2. Lookup via BlockStateModelSet particle material
        try {
            Block block = BuiltInRegistries.BLOCK.getOptional(woodId).orElse(null);
            if (block == null || block == Blocks.AIR) {
                Item item = BuiltInRegistries.ITEM.getOptional(woodId).orElse(null);
                if (item instanceof BlockItem bi) {
                    block = bi.getBlock();
                }
            }
            if (block != null && block != Blocks.AIR) {
                ModelManager mm = Minecraft.getInstance().getModelManager();
                if (mm != null && mm.getBlockStateModelSet() != null) {
                    Material.Baked mat = mm.getBlockStateModelSet().getParticleMaterial(block.defaultBlockState());
                    if (mat != null && isValidSprite(mat.sprite())) {
                        return mat.sprite();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 3. Fallback to oak planks in atlas
        if (atlas != null) {
            TextureAtlasSprite oakSprite = atlas.getSprite(Identifier.fromNamespaceAndPath("minecraft", "block/oak_planks"));
            if (isValidSprite(oakSprite)) {
                return oakSprite;
            }
        }

        // 4. Missing model particle as final safeguard
        try {
            return Minecraft.getInstance().getModelManager().getBlockStateModelSet().missingModel().particleMaterial().sprite();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isValidSprite(@Nullable TextureAtlasSprite sprite) {
        return sprite != null && !sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation());
    }

    private @Nullable TextureAtlasSprite resolveOverlaySprite(Direction dir) {
        TextureAtlas atlas = getBlockAtlas();
        if (atlas == null) return null;

        if (furnitureType == FurnitureType.CRAFTING_TABLE) {
            return switch (dir) {
                case UP -> resolveAtlasSprite(
                        atlas,
                        Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "block/crafting_table_top_overlay"),
                        Identifier.fromNamespaceAndPath("minecraft", "block/crafting_table_top")
                );
                case NORTH, WEST -> resolveAtlasSprite(
                        atlas,
                        Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "block/crafting_table_front_overlay"),
                        Identifier.fromNamespaceAndPath("minecraft", "block/crafting_table_front")
                );
                case SOUTH, EAST -> resolveAtlasSprite(
                        atlas,
                        Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "block/crafting_table_side_overlay"),
                        Identifier.fromNamespaceAndPath("minecraft", "block/crafting_table_side")
                );
                case DOWN -> null;
            };
        } else if (furnitureType == FurnitureType.BOOKSHELF) {
            return switch (dir) {
                case NORTH, SOUTH, EAST, WEST -> resolveAtlasSprite(
                        atlas,
                        Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "block/bookshelf_overlay"),
                        Identifier.fromNamespaceAndPath("minecraft", "block/bookshelf")
                );
                case UP, DOWN -> null;
            };
        }
        return null;
    }

    private @Nullable TextureAtlasSprite resolveAtlasSprite(TextureAtlas atlas, Identifier id, Identifier fallbackId) {
        TextureAtlasSprite sprite = atlas.getSprite(id);
        if (isValidSprite(sprite)) {
            return sprite;
        }
        if (fallbackId != null) {
            sprite = atlas.getSprite(fallbackId);
            if (isValidSprite(sprite)) {
                return sprite;
            }
        }
        return null;
    }

    // --- BlockStateModel Implementation ---

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> outParts) {
        Direction facing = (blockFacing != null) ? blockFacing : Direction.NORTH;
        outParts.add(getOrBuildModelData(new ModelCacheKey(DynamicWoodBlockEntity.DEFAULT_WOOD, facing)).part());
    }

    @Override
    public Material.Baked particleMaterial() {
        Direction facing = (blockFacing != null) ? blockFacing : Direction.NORTH;
        return getOrBuildModelData(new ModelCacheKey(DynamicWoodBlockEntity.DEFAULT_WOOD, facing)).particleMaterial();
    }

    @Override
    public int materialFlags() {
        return 0;
    }

    // --- FabricBlockStateModel Implementation ---

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
        Identifier woodId = DynamicWoodBlockEntity.DEFAULT_WOOD;
        if (level != null && pos != null) {
            Object renderData = level.getBlockEntityRenderData(pos);
            if (renderData instanceof Identifier id) {
                woodId = id;
            } else {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof DynamicWoodBlockEntity dynamicBe) {
                    woodId = dynamicBe.getWoodType();
                }
            }
        }

        Direction facing = (blockFacing != null) ? blockFacing : Direction.NORTH;
        if (furnitureType == FurnitureType.LADDER && state != null && state.hasProperty(LadderBlock.FACING)) {
            facing = state.getValue(LadderBlock.FACING);
        }

        CachedModelData data = getOrBuildModelData(new ModelCacheKey(woodId, facing));
        if (data.mesh() != null) {
            data.mesh().outputTo(emitter);
        } else {
            VanillaBlockModelPartEncoder.emitQuads(data.part(), emitter, cullTest);
        }
    }

    // --- ItemModel Implementation ---

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, ClientLevel level, ItemOwner owner, int seed) {
        Identifier woodId = stack.get(ModDataComponents.WOOD_TYPE);
        if (woodId == null) {
            woodId = DynamicWoodBlockEntity.DEFAULT_WOOD;
        }

        renderState.appendModelIdentityElement(this);
        renderState.appendModelIdentityElement(woodId);

        CachedModelData data = getOrBuildModelData(new ModelCacheKey(woodId, Direction.NORTH));

        ItemStackRenderState.LayerRenderState layer = renderState.newLayer();

        if (stack.hasFoil()) {
            ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.STANDARD;
            layer.setFoilType(foilType);
            renderState.setAnimated();
            renderState.appendModelIdentityElement(foilType);
        }

        layer.setExtents(data.extents());
        layer.setLocalTransform(this.itemTransformation != null ? this.itemTransformation : new Matrix4f());

        if (itemRenderProperties != null) {
            itemRenderProperties.applyToLayer(layer, displayContext);
        } else {
            layer.setUsesBlockLight(true);
            ItemTransform transform;
            if (furnitureType == FurnitureType.LADDER) {
                transform = switch (displayContext) {
                    case GUI -> DEFAULT_LADDER_GUI_TRANSFORM;
                    case GROUND -> DEFAULT_LADDER_GROUND_TRANSFORM;
                    case FIXED -> DEFAULT_LADDER_FIXED_TRANSFORM;
                    case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> DEFAULT_LADDER_THIRD_PERSON_TRANSFORM;
                    case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> DEFAULT_LADDER_FIRST_PERSON_TRANSFORM;
                    default -> ItemTransform.NO_TRANSFORM;
                };
            } else {
                transform = switch (displayContext) {
                    case GUI -> DEFAULT_GUI_TRANSFORM;
                    case GROUND -> DEFAULT_GROUND_TRANSFORM;
                    case FIXED -> DEFAULT_FIXED_TRANSFORM;
                    case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> DEFAULT_THIRD_PERSON_TRANSFORM;
                    case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> DEFAULT_FIRST_PERSON_TRANSFORM;
                    default -> ItemTransform.NO_TRANSFORM;
                };
            }
            layer.setItemTransform(transform);
        }

        layer.setParticleMaterial(data.particleMaterial());

        List<BakedQuad> quadList = layer.prepareQuadList();
        quadList.addAll(data.itemQuads());
    }

    // --- Inner BlockStateModelPart ---

    public static class DynamicBlockStateModelPart implements BlockStateModelPart, FabricBlockStateModelPart {
        private final Map<Direction, List<BakedQuad>> culledQuads;
        private final List<BakedQuad> unculledQuads;
        private final Material.Baked particleMaterial;

        public DynamicBlockStateModelPart(Map<Direction, List<BakedQuad>> culledQuads, List<BakedQuad> unculledQuads, Material.Baked particleMaterial) {
            this.culledQuads = culledQuads;
            this.unculledQuads = unculledQuads;
            this.particleMaterial = particleMaterial;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            if (direction == null) {
                return unculledQuads;
            }
            return culledQuads.getOrDefault(direction, List.of());
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public Material.Baked particleMaterial() {
            return particleMaterial;
        }

        @Override
        public int materialFlags() {
            return 0;
        }

        @Override
        public void emitQuads(QuadEmitter emitter, Predicate<@Nullable Direction> cullTest) {
            for (Direction dir : Direction.values()) {
                if (cullTest.test(dir)) {
                    List<BakedQuad> quads = culledQuads.get(dir);
                    if (quads != null) {
                        for (BakedQuad q : quads) {
                            emitter.fromBakedQuad(q);
                            emitter.cullFace(dir);
                            emitter.emit();
                        }
                    }
                }
            }
            for (BakedQuad q : unculledQuads) {
                emitter.fromBakedQuad(q);
                emitter.emit();
            }
        }
    }
}
