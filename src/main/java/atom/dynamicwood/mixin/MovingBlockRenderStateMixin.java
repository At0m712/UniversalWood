package atom.dynamicwood.mixin;

import atom.dynamicwood.block.entity.DynamicWoodBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MovingBlockRenderState.class)
public class MovingBlockRenderStateMixin {
    @Shadow
    public BlockPos blockPos;

    @Inject(method = "getBlockEntity", at = @At("HEAD"), cancellable = true)
    private void dynamicwood$getDynamicWoodBlockEntity(BlockPos pos, CallbackInfoReturnable<BlockEntity> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.level != null) {
            BlockPos targetPos = (pos != null) ? pos : this.blockPos;
            if (targetPos != null) {
                BlockEntity be = client.level.getBlockEntity(targetPos);
                if (be instanceof DynamicWoodBlockEntity) {
                    cir.setReturnValue(be);
                }
            }
        }
    }
}
