package atom.dynamicwood.mixin;

import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import atom.dynamicwood.block.properties.DynamicWoodProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireBlock.class)
public class FireBlockMixin {
    @Inject(method = "getIgniteOdds(Lnet/minecraft/world/level/block/state/BlockState;)I", at = @At("RETURN"), cancellable = true)
    private void dynamicwood$overrideIgniteOdds(BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (state.hasProperty(DynamicWoodProperties.FIREPROOF) && state.getValue(DynamicWoodProperties.FIREPROOF)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getBurnOdds(Lnet/minecraft/world/level/block/state/BlockState;)I", at = @At("RETURN"), cancellable = true)
    private void dynamicwood$overrideBurnOdds(BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (state.hasProperty(DynamicWoodProperties.FIREPROOF) && state.getValue(DynamicWoodProperties.FIREPROOF)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
    private void dynamicwood$preventBurnOut(Level level, BlockPos pos, int i, RandomSource random, int j, CallbackInfo ci) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(DynamicWoodProperties.FIREPROOF) && state.getValue(DynamicWoodProperties.FIREPROOF)) {
            ci.cancel();
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DynamicWoodBlockEntity dynamicBe && dynamicBe.isFireproof()) {
            ci.cancel();
        }
    }
}
