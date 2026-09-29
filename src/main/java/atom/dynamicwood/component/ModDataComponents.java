package atom.dynamicwood.component;

import atom.dynamicwood.DynamicWoodMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModDataComponents {
    public static final DataComponentType<Identifier> WOOD_TYPE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(DynamicWoodMod.MOD_ID, "wood_type"),
            DataComponentType.<Identifier>builder()
                    .persistent(Identifier.CODEC)
                    .networkSynchronized(Identifier.STREAM_CODEC)
                    .build()
    );

    private ModDataComponents() {
    }

    public static void initialize() {
        // Triggers class loading and registration
    }
}
