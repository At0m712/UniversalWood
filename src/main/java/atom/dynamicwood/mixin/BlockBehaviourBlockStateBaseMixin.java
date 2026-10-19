package atom.dynamicwood.mixin;

import atom.dynamicwood.block.properties.DynamicWoodProperties;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockBehaviourBlockStateBaseMixin {
    @Inject(method = "ignitedByLava", at = @At("HEAD"), cancellable = true)
    private void dynamicwood$overrideIgnitedByLava(CallbackInfoReturnable<Boolean> cir) {
        BlockState state = (BlockState) (Object) this;
        if (state.hasProperty(DynamicWoodProperties.FIREPROOF) && state.getValue(DynamicWoodProperties.FIREPROOF)) {
            cir.setReturnValue(false);
        }
    }
}
