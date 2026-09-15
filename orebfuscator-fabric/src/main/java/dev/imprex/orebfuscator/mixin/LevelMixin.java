package dev.imprex.orebfuscator.mixin;

import dev.imprex.orebfuscator.event.DeobfuscationEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {

  @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
      at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
  public void orebfuscator$setBlock(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit,
      CallbackInfoReturnable<Boolean> cir) {
    if ((Object) this instanceof ServerLevel level) {
      DeobfuscationEvents.SET_BLOCK.invoker().onSetBlock(level, pos);
    }
  }
}
