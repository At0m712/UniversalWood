package atom.dynamicwood.client;

import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class WoodColorResolver {
    private static final int DEFAULT_OAK_ARGB = 0xFFAE8E5C;
    private static final float BRIGHTNESS_BOOST = 1.18f;
    private static final ConcurrentHashMap<Identifier, Integer> CACHE = new ConcurrentHashMap<>();

    private WoodColorResolver() {
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static int getTintArgb(@Nullable Identifier woodId) {
        Identifier key = (woodId != null) ? woodId : DynamicWoodBlockEntity.DEFAULT_WOOD;
        return CACHE.computeIfAbsent(key, WoodColorResolver::computeTintArgb);
    }

    public static int getTintRgb(@Nullable Identifier woodId) {
        return getTintArgb(woodId) & 0x00FFFFFF;
    }

    private static int computeTintArgb(Identifier woodId) {
        // 1. Sample the plank texture directly from ResourceManager (works for vanilla, modded woods, and resource packs)
        Integer sampled = samplePlankTexture(woodId);
        if (sampled != null) {
            return sampled;
        }

        // 2. Fallback to the plank block's MapColor
        Block block = resolvePlankBlock(woodId);
        if (block != null && block != Blocks.AIR) {
            int mapCol = block.defaultMapColor().col;
            if (mapCol != 0) {
                return boostColor((mapCol >> 16) & 0xFF, (mapCol >> 8) & 0xFF, mapCol & 0xFF);
            }
        }

        return DEFAULT_OAK_ARGB;
    }

    private static @Nullable Integer samplePlankTexture(Identifier woodId) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null) {
                return null;
            }
            ResourceManager rm = client.getResourceManager();
            if (rm == null) {
                return null;
            }

            String path = woodId.getPath();
            if (path.startsWith("block/")) {
                path = path.substring(6);
            }
            Identifier textureId = Identifier.fromNamespaceAndPath(woodId.getNamespace(), "textures/block/" + path + ".png");
            Optional<Resource> res = rm.getResource(textureId);
            if (res.isEmpty()) {
                return null;
            }

            try (InputStream is = res.get().open(); NativeImage image = NativeImage.read(is)) {
                long sumR = 0;
                long sumG = 0;
                long sumB = 0;
                long count = 0;
                int w = image.getWidth();
                int h = image.getHeight();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int argb = image.getPixel(x, y);
                        int a = (argb >>> 24) & 0xFF;
                        if (a < 128) continue;
                        sumR += (argb >>> 16) & 0xFF;
                        sumG += (argb >>> 8) & 0xFF;
                        sumB += argb & 0xFF;
                        count++;
                    }
                }
                if (count > 0) {
                    int avgR = (int) (sumR / count);
                    int avgG = (int) (sumG / count);
                    int avgB = (int) (sumB / count);
                    return boostColor(avgR, avgG, avgB);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static int boostColor(int r, int g, int b) {
        int br = Math.min(255, Math.round(r * BRIGHTNESS_BOOST));
        int bg = Math.min(255, Math.round(g * BRIGHTNESS_BOOST));
        int bb = Math.min(255, Math.round(b * BRIGHTNESS_BOOST));
        return 0xFF000000 | (br << 16) | (bg << 8) | bb;
    }

    private static @Nullable Block resolvePlankBlock(Identifier woodId) {
        Block block = BuiltInRegistries.BLOCK.getOptional(woodId).orElse(null);
        if (block == null || block == Blocks.AIR) {
            String path = woodId.getPath();
            if (path.startsWith("block/")) {
                Identifier cleanId = Identifier.fromNamespaceAndPath(woodId.getNamespace(), path.substring(6));
                block = BuiltInRegistries.BLOCK.getOptional(cleanId).orElse(null);
            }
        }
        if (block == null || block == Blocks.AIR) {
            Item item = BuiltInRegistries.ITEM.getOptional(woodId).orElse(null);
            if (item instanceof BlockItem bi) {
                block = bi.getBlock();
            }
        }
        return block;
    }
}
