package atom.dynamicwood.client;

import atom.dynamicwood.DynamicWoodMod;
import atom.dynamicwood.block.entity.DynamicChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DynamicChestRenderer implements BlockEntityRenderer<DynamicChestBlockEntity, DynamicChestRenderState> {
    public static final Identifier NORMAL_WOOD_TEX = Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "textures/entity/chest/dynamic_normal.png");
    public static final Identifier NORMAL_LOCK_TEX = Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "textures/entity/chest/dynamic_normal_lock.png");
    public static final Identifier LEFT_WOOD_TEX = Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "textures/entity/chest/dynamic_left.png");
    public static final Identifier LEFT_LOCK_TEX = Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "textures/entity/chest/dynamic_left_lock.png");
    public static final Identifier RIGHT_WOOD_TEX = Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "textures/entity/chest/dynamic_right.png");
    public static final Identifier RIGHT_LOCK_TEX = Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "textures/entity/chest/dynamic_right_lock.png");

    private final ChestModel singleModel;
    private final ChestModel doubleLeftModel;
    private final ChestModel doubleRightModel;

    public DynamicChestRenderer(BlockEntityRendererProvider.Context context) {
        this.singleModel = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
        this.doubleLeftModel = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT));
        this.doubleRightModel = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT));
    }

    @Override
    public DynamicChestRenderState createRenderState() {
        return new DynamicChestRenderState();
    }

    @Override
    public void extractRenderState(
            DynamicChestBlockEntity blockEntity,
            DynamicChestRenderState renderState,
            float partialTick,
            Vec3 cameraPos,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, breakProgress);

        Level level = blockEntity.getLevel();
        boolean hasLevel = level != null;
        BlockState state = hasLevel
                ? blockEntity.getBlockState()
                : DynamicWoodMod.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);

        renderState.type = state.hasProperty(ChestBlock.TYPE) ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        renderState.facing = state.hasProperty(ChestBlock.FACING) ? state.getValue(ChestBlock.FACING) : Direction.SOUTH;
        renderState.woodType = blockEntity.getWoodType();

        DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> neighborCombineResult;
        if (hasLevel && state.getBlock() instanceof ChestBlock chestBlock) {
            neighborCombineResult = chestBlock.combine(state, level, blockEntity.getBlockPos(), true);
        } else {
            neighborCombineResult = DoubleBlockCombiner.Combiner::acceptNone;
        }

        renderState.open = neighborCombineResult.apply(ChestBlock.opennessCombiner(blockEntity)).get(partialTick);
        if (renderState.type != ChestType.SINGLE) {
            renderState.lightCoords = neighborCombineResult.apply(new BrightnessCombiner<>()).applyAsInt(renderState.lightCoords);
        }
    }

    @Override
    public void submit(
            DynamicChestRenderState renderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState
    ) {
        poseStack.pushPose();
        poseStack.mulPose(ChestRenderer.modelTransformation(renderState.facing));

        float open = renderState.open;
        open = 1.0f - open;
        open = 1.0f - open * open * open;

        ChestModel model;
        Identifier woodTex;
        Identifier lockTex;
        if (renderState.type == ChestType.LEFT) {
            model = this.doubleLeftModel;
            woodTex = LEFT_WOOD_TEX;
            lockTex = LEFT_LOCK_TEX;
        } else if (renderState.type == ChestType.RIGHT) {
            model = this.doubleRightModel;
            woodTex = RIGHT_WOOD_TEX;
            lockTex = RIGHT_LOCK_TEX;
        } else {
            model = this.singleModel;
            woodTex = NORMAL_WOOD_TEX;
            lockTex = NORMAL_LOCK_TEX;
        }

        int tintArgb = WoodColorResolver.getTintArgb(renderState.woodType);

        // Pass 1: Tinted wood body & lid
        submitNodeCollector.submitModel(
                model,
                open,
                poseStack,
                RenderTypes.entityCutout(woodTex),
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                tintArgb,
                null,
                0
        );

        // Pass 2: Untinted metallic iron latch
        submitNodeCollector.submitModel(
                model,
                open,
                poseStack,
                RenderTypes.entityCutout(lockTex),
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF,
                null,
                0
        );

        poseStack.popPose();
    }
}
