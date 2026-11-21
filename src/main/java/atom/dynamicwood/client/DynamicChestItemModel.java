package atom.dynamicwood.client;

import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import atom.dynamicwood.component.ModDataComponents;
import com.google.common.base.Suppliers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class DynamicChestItemModel implements ItemModel {
    private static final ItemTransform GUI_TRANSFORM = new ItemTransform(
            new Vector3f(30, 45, 0),
            new Vector3f(0, 0, 0),
            new Vector3f(0.625f, 0.625f, 0.625f)
    );
    private static final ItemTransform GROUND_TRANSFORM = new ItemTransform(
            new Vector3f(0, 0, 0),
            new Vector3f(0, 3f / 16f, 0),
            new Vector3f(0.25f, 0.25f, 0.25f)
    );
    private static final ItemTransform FIXED_TRANSFORM = new ItemTransform(
            new Vector3f(0, 180, 0),
            new Vector3f(0, 0, 0),
            new Vector3f(0.5f, 0.5f, 0.5f)
    );
    private static final ItemTransform THIRD_PERSON_TRANSFORM = new ItemTransform(
            new Vector3f(75, 45, 0),
            new Vector3f(0, 2.5f / 16f, 0),
            new Vector3f(0.375f, 0.375f, 0.375f)
    );
    private static final ItemTransform FIRST_PERSON_TRANSFORM = new ItemTransform(
            new Vector3f(0, 225, 0),
            new Vector3f(0, 0, 0),
            new Vector3f(0.4f, 0.4f, 0.4f)
    );

    private final DynamicChestSpecialRenderer specialRenderer = new DynamicChestSpecialRenderer();
    private Supplier<Vector3fc[]> extents = createExtentsSupplier();
    private @Nullable ModelRenderProperties itemRenderProperties;

    private Supplier<Vector3fc[]> createExtentsSupplier() {
        return Suppliers.memoize(() -> {
            Set<Vector3fc> set = new HashSet<>();
            specialRenderer.getExtents(set::add);
            return set.toArray(new Vector3fc[0]);
        });
    }

    public void clearCache() {
        specialRenderer.clearCache();
        extents = createExtentsSupplier();
    }

    @Override
    public void update(
            ItemStackRenderState renderState,
            ItemStack stack,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed
    ) {
        Identifier woodId = stack.get(ModDataComponents.WOOD_TYPE);
        if (woodId == null) {
            woodId = DynamicWoodBlockEntity.DEFAULT_WOOD;
        }

        renderState.appendModelIdentityElement(this);
        renderState.appendModelIdentityElement(woodId);

        ItemStackRenderState.LayerRenderState layer = renderState.newLayer();

        if (stack.hasFoil()) {
            ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.STANDARD;
            layer.setFoilType(foilType);
            renderState.setAnimated();
            renderState.appendModelIdentityElement(foilType);
        }

        layer.setExtents(extents);
        layer.setLocalTransform(new Matrix4f());

        if (itemRenderProperties != null) {
            itemRenderProperties.applyToLayer(layer, displayContext);
        } else {
            layer.setUsesBlockLight(true);
            ItemTransform transform = switch (displayContext) {
                case GUI -> GUI_TRANSFORM;
                case GROUND -> GROUND_TRANSFORM;
                case FIXED -> FIXED_TRANSFORM;
                case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> THIRD_PERSON_TRANSFORM;
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> FIRST_PERSON_TRANSFORM;
                default -> ItemTransform.NO_TRANSFORM;
            };
            layer.setItemTransform(transform);
        }

        try {
            TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
            String path = woodId.getPath();
            if (!path.startsWith("block/")) {
                path = "block/" + path;
            }
            TextureAtlasSprite sprite = atlas.getSprite(Identifier.fromNamespaceAndPath(woodId.getNamespace(), path));
            if (sprite != null) {
                layer.setParticleMaterial(new Material.Baked(sprite, false));
            }
        } catch (Exception ignored) {
        }

        layer.setupSpecialModel(specialRenderer, woodId);
    }
}
