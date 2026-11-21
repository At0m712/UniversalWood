package atom.dynamicwood.block.entity;

import net.minecraft.resources.Identifier;

public interface DynamicWoodHolder {
    Identifier getWoodType();

    void setWoodType(Identifier woodType);

    boolean isFireproof();
}
