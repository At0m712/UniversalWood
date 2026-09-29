package atom.dynamicwood.client;

import atom.dynamicwood.DynamicWoodMod;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class DynamicWoodReloadListener implements SimpleSynchronousResourceReloadListener {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "model_cache_reloader");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        DynamicWoodMod.LOGGER.info("[DynamicWoodFurniture] Resource reload detected (F3+T or pack reload). Invalidation of dynamic wood model cache...");
        DynamicWoodModel.clearAllCaches();
        DynamicWoodMod.LOGGER.info("[DynamicWoodFurniture] Dynamic wood model cache successfully invalidated.");
    }
}
