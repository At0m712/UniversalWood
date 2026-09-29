package atom.dynamicwood.item;

import atom.dynamicwood.DynamicWoodMod;
import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import atom.dynamicwood.component.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public final class DynamicWoodItemGroups {
    public static final TagKey<Item> C_WOODEN_PLANKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "wooden_planks"));

    public static final CreativeModeTab DYNAMIC_WOOD_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "dynamic_wood_tab"),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 7)
                    .title(Component.translatable("itemGroup.dynamicwood.dynamic_wood_tab"))
                    .icon(() -> {
                        ItemStack icon = new ItemStack(DynamicWoodMod.CRAFTING_TABLE_ITEM);
                        icon.set(ModDataComponents.WOOD_TYPE, Identifier.fromNamespaceAndPath("minecraft", "oak_planks"));
                        return icon;
                    })
                    .displayItems((parameters, output) -> {
                        Set<Identifier> plankIds = new LinkedHashSet<>();

                        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.PLANKS)) {
                            Identifier id = holder.unwrapKey().map(ResourceKey::identifier)
                                    .orElseGet(() -> BuiltInRegistries.ITEM.getKey(holder.value()));
                            plankIds.add(id);
                        }

                        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(C_WOODEN_PLANKS)) {
                            Identifier id = holder.unwrapKey().map(ResourceKey::identifier)
                                    .orElseGet(() -> BuiltInRegistries.ITEM.getKey(holder.value()));
                            plankIds.add(id);
                        }

                        if (plankIds.isEmpty()) {
                            plankIds.add(Identifier.fromNamespaceAndPath("minecraft", "oak_planks"));
                        }

                        for (Identifier woodId : plankIds) {
                            ItemStack craftingTable = new ItemStack(DynamicWoodMod.CRAFTING_TABLE_ITEM);
                            craftingTable.set(ModDataComponents.WOOD_TYPE, woodId);
                            output.accept(craftingTable);

                            ItemStack bookshelf = new ItemStack(DynamicWoodMod.BOOKSHELF_ITEM);
                            bookshelf.set(ModDataComponents.WOOD_TYPE, woodId);
                            output.accept(bookshelf);

                            if (!isOak(woodId)) {
                                ItemStack ladder = new ItemStack(DynamicWoodMod.LADDER_ITEM);
                                ladder.set(ModDataComponents.WOOD_TYPE, woodId);
                                output.accept(ladder);
                            }
                        }
                    })
                    .build()
    );

    private static boolean isOak(@Nullable Identifier woodId) {
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

    private DynamicWoodItemGroups() {
    }

    public static void initialize() {
        // Triggers class loading and registration
    }
}
