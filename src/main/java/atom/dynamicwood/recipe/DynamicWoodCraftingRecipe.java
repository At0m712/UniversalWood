package atom.dynamicwood.recipe;

import atom.dynamicwood.DynamicWoodMod;
import atom.dynamicwood.component.ModDataComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DynamicWoodCraftingRecipe extends CustomRecipe {
    public static final TagKey<Item> C_WOODEN_PLANKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "wooden_planks"));
    public static final TagKey<Item> C_PLANKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "planks"));
    public static final TagKey<Item> C_BOOKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "books"));
    public static final TagKey<Item> C_STICKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "sticks"));
    public static final TagKey<Item> C_WOOD_STICKS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "wood_sticks"));

    public static final RecipeSerializer<DynamicWoodCraftingRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(new DynamicWoodCraftingRecipe()),
            StreamCodec.unit(new DynamicWoodCraftingRecipe())
    );

    public DynamicWoodCraftingRecipe() {
        super();
    }

    private static boolean isOak(@Nullable Identifier woodId) {
        if (woodId == null) {
            return true;
        }
        String path = woodId.getPath();
        if (path.startsWith("block/")) {
            path = path.substring(6);
        }
        return woodId.getNamespace().equals("minecraft") && path.equals("oak_planks");
    }

    public static boolean isPlank(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ItemTags.PLANKS) || stack.is(C_WOODEN_PLANKS) || stack.is(C_PLANKS);
    }

    public static boolean isBook(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.BOOK) || stack.is(C_BOOKS);
    }

    public static boolean isStick(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.STICK) || stack.is(C_STICKS) || stack.is(C_WOOD_STICKS);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return matchesCraftingTable(input) || matchesBookshelf(input) || matchesLadder(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        if (matchesCraftingTable(input)) {
            List<ItemStack> planks = getCraftingTablePlanks(input);
            if (planks.size() == 4) {
                Item firstPlank = planks.get(0).getItem();
                boolean homogeneous = true;
                for (int i = 1; i < 4; i++) {
                    if (!planks.get(i).is(firstPlank)) {
                        homogeneous = false;
                        break;
                    }
                }

                if (homogeneous) {
                    ItemStack result = new ItemStack(DynamicWoodMod.CRAFTING_TABLE_ITEM);
                    Identifier woodId = BuiltInRegistries.ITEM.getKey(firstPlank);
                    result.set(ModDataComponents.WOOD_TYPE, woodId);
                    return result;
                } else {
                    return new ItemStack(Items.CRAFTING_TABLE);
                }
            }
        }

        if (matchesBookshelf(input)) {
            List<ItemStack> planks = getBookshelfPlanks(input);
            if (planks.size() == 6) {
                Item firstPlank = planks.get(0).getItem();
                boolean homogeneous = true;
                for (int i = 1; i < 6; i++) {
                    if (!planks.get(i).is(firstPlank)) {
                        homogeneous = false;
                        break;
                    }
                }

                if (homogeneous) {
                    ItemStack result = new ItemStack(DynamicWoodMod.BOOKSHELF_ITEM);
                    Identifier woodId = BuiltInRegistries.ITEM.getKey(firstPlank);
                    result.set(ModDataComponents.WOOD_TYPE, woodId);
                    return result;
                } else {
                    return new ItemStack(Items.BOOKSHELF);
                }
            }
        }

        if (matchesLadder(input)) {
            return assembleLadder(input);
        }

        return ItemStack.EMPTY;
    }

    private boolean matchesCraftingTable(CraftingInput input) {
        int nonAirCount = 0;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (!stack.isEmpty()) {
                    nonAirCount++;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (nonAirCount != 4) return false;
        if (maxX - minX != 1 || maxY - minY != 1) return false;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (!isPlank(input.getItem(x, y))) {
                    return false;
                }
            }
        }

        return true;
    }

    private List<ItemStack> getCraftingTablePlanks(CraftingInput input) {
        List<ItemStack> planks = new ArrayList<>(4);
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (!stack.isEmpty() && isPlank(stack)) {
                    planks.add(stack);
                }
            }
        }
        return planks;
    }

    private boolean matchesBookshelf(CraftingInput input) {
        if (input.width() < 3 || input.height() < 3) return false;

        int nonAirCount = 0;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (!stack.isEmpty()) {
                    nonAirCount++;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (nonAirCount != 9) return false;
        if (maxX - minX != 2 || maxY - minY != 2) return false;

        for (int x = minX; x <= maxX; x++) {
            if (!isPlank(input.getItem(x, minY))) return false;
        }

        for (int x = minX; x <= maxX; x++) {
            if (!isBook(input.getItem(x, minY + 1))) return false;
        }

        for (int x = minX; x <= maxX; x++) {
            if (!isPlank(input.getItem(x, minY + 2))) return false;
        }

        return true;
    }

    private List<ItemStack> getBookshelfPlanks(CraftingInput input) {
        List<ItemStack> planks = new ArrayList<>(6);
        int minY = Integer.MAX_VALUE;
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                if (!input.getItem(x, y).isEmpty()) {
                    if (y < minY) minY = y;
                }
            }
        }

        for (int x = 0; x < input.width(); x++) {
            ItemStack top = input.getItem(x, minY);
            if (!top.isEmpty() && isPlank(top)) {
                planks.add(top);
            }
        }
        for (int x = 0; x < input.width(); x++) {
            ItemStack bottom = input.getItem(x, minY + 2);
            if (!bottom.isEmpty() && isPlank(bottom)) {
                planks.add(bottom);
            }
        }

        return planks;
    }

    private boolean matchesLadder(CraftingInput input) {
        if (input.width() < 3 || input.height() < 3) return false;

        int nonAirCount = 0;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                ItemStack stack = input.getItem(x, y);
                if (!stack.isEmpty()) {
                    nonAirCount++;
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (nonAirCount != 7) return false;
        if (maxX - minX != 2 || maxY - minY != 2) return false;

        if (!input.getItem(minX + 1, minY).isEmpty()) return false;
        if (!input.getItem(minX + 1, minY + 2).isEmpty()) return false;

        ItemStack topLeft = input.getItem(minX, minY);
        ItemStack topRight = input.getItem(minX + 2, minY);
        ItemStack midLeft = input.getItem(minX, minY + 1);
        ItemStack midCenter = input.getItem(minX + 1, minY + 1);
        ItemStack midRight = input.getItem(minX + 2, minY + 1);
        ItemStack botLeft = input.getItem(minX, minY + 2);
        ItemStack botRight = input.getItem(minX + 2, minY + 2);

        boolean allPlanks = isPlank(topLeft) && isPlank(topRight)
                && isPlank(midLeft) && isPlank(midCenter) && isPlank(midRight)
                && isPlank(botLeft) && isPlank(botRight);
        if (allPlanks) return true;

        boolean sticksWithPlank = isStick(topLeft) && isStick(topRight)
                && isStick(midLeft) && isPlank(midCenter) && isStick(midRight)
                && isStick(botLeft) && isStick(botRight);
        if (sticksWithPlank) return true;

        boolean allSticks = isStick(topLeft) && isStick(topRight)
                && isStick(midLeft) && isStick(midCenter) && isStick(midRight)
                && isStick(botLeft) && isStick(botRight);
        return allSticks;
    }

    private ItemStack assembleLadder(CraftingInput input) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;

        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                if (!input.getItem(x, y).isEmpty()) {
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                }
            }
        }

        ItemStack topLeft = input.getItem(minX, minY);
        ItemStack topRight = input.getItem(minX + 2, minY);
        ItemStack midLeft = input.getItem(minX, minY + 1);
        ItemStack midCenter = input.getItem(minX + 1, minY + 1);
        ItemStack midRight = input.getItem(minX + 2, minY + 1);
        ItemStack botLeft = input.getItem(minX, minY + 2);
        ItemStack botRight = input.getItem(minX + 2, minY + 2);

        // Pattern 1: All 7 are planks
        if (isPlank(topLeft) && isPlank(topRight)
                && isPlank(midLeft) && isPlank(midCenter) && isPlank(midRight)
                && isPlank(botLeft) && isPlank(botRight)) {

            Item firstPlank = topLeft.getItem();
            boolean homogeneous = topRight.is(firstPlank) && midLeft.is(firstPlank)
                    && midCenter.is(firstPlank) && midRight.is(firstPlank)
                    && botLeft.is(firstPlank) && botRight.is(firstPlank);

            if (homogeneous) {
                Identifier woodId = BuiltInRegistries.ITEM.getKey(firstPlank);
                if (isOak(woodId)) {
                    return new ItemStack(Items.LADDER, 8);
                }
                ItemStack result = new ItemStack(DynamicWoodMod.LADDER_ITEM, 8);
                result.set(ModDataComponents.WOOD_TYPE, woodId);
                return result;
            } else {
                return new ItemStack(Items.LADDER, 8);
            }
        }

        // Pattern 2: 6 sticks + 1 plank in center
        if (isPlank(midCenter) && isStick(topLeft) && isStick(topRight)
                && isStick(midLeft) && isStick(midRight)
                && isStick(botLeft) && isStick(botRight)) {

            Identifier woodId = BuiltInRegistries.ITEM.getKey(midCenter.getItem());
            if (isOak(woodId)) {
                return new ItemStack(Items.LADDER, 4);
            }
            ItemStack result = new ItemStack(DynamicWoodMod.LADDER_ITEM, 4);
            result.set(ModDataComponents.WOOD_TYPE, woodId);
            return result;
        }

        // Pattern 3: All 7 are sticks
        return new ItemStack(Items.LADDER, 3);
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
