package atom.dynamicwood.client;

import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import atom.dynamicwood.component.ModDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class DynamicChestSpecialRenderer implements SpecialModelRenderer<Identifier> {
    private @Nullable ChestModel model;

    private ChestModel getOrCreateModel() {
        if (this.model == null) {
            this.model = new ChestModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.CHEST));
        }
        return this.model;
    }

    public void clearCache() {
        this.model = null;
    }

    @Override
    public void submit(
            @Nullable Identifier woodId,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor
    ) {
        ChestModel chestModel = getOrCreateModel();
        int tintArgb = WoodColorResolver.getTintArgb(woodId);

        // Pass 1: Tinted wood body & lid
        submitNodeCollector.submitModel(
                chestModel,
                0.0f,
                poseStack,
                RenderTypes.entityCutout(DynamicChestRenderer.NORMAL_WOOD_TEX),
                lightCoords,
                overlayCoords,
                tintArgb,
                null,
                outlineColor
        );

        // Pass 2: Untinted metallic iron latch
        submitNodeCollector.submitModel(
                chestModel,
                0.0f,
                poseStack,
                RenderTypes.entityCutout(DynamicChestRenderer.NORMAL_LOCK_TEX),
                lightCoords,
                overlayCoords,
                0xFFFFFFFF,
                null,
                outlineColor
        );
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        ChestModel chestModel = getOrCreateModel();
        PoseStack poseStack = new PoseStack();
        chestModel.setupAnim(0.0f);
        chestModel.root().getExtentsForGui(poseStack, output);
    }

    @Override
    public Identifier extractArgument(ItemStack stack) {
        Identifier woodId = stack.get(ModDataComponents.WOOD_TYPE);
        return woodId != null ? woodId : DynamicWoodBlockEntity.DEFAULT_WOOD;
    }
}
