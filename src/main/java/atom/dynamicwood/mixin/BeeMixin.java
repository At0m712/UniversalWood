package atom.dynamicwood.mixin;

import atom.dynamicwood.block.entity.DynamicBeehiveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Bee.class)
public abstract class BeeMixin extends Animal {
    @Shadow
    private @Nullable BlockPos hivePos;

    protected BeeMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    private boolean isTooFarAway(BlockPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "getBeehiveBlockEntity", at = @At("RETURN"), cancellable = true)
    private void dynamicwood$supportDynamicBeehive(CallbackInfoReturnable<@Nullable BeehiveBlockEntity> cir) {
        if (cir.getReturnValue() == null && this.hivePos != null && !this.isTooFarAway(this.hivePos)) {
            BlockEntity be = this.level().getBlockEntity(this.hivePos);
            if (be instanceof DynamicBeehiveBlockEntity dynamicBeehive) {
                cir.setReturnValue(dynamicBeehive);
            }
        }
    }
}
