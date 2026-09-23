package dev.imprex.orebfuscator.mixin;

import dev.imprex.orebfuscator.event.DeobfuscationEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

  @Shadow
  protected ServerLevel level;

  @Inject(
      method = "handleBlockBreakAction",
      at = @At(
          value = "FIELD",
          target = "Lnet/minecraft/server/level/ServerPlayerGameMode;isDestroyingBlock:Z",
          opcode = Opcodes.PUTFIELD,
          ordinal = 0))
  public void orebfuscator$handleBlockBreakAction(BlockPos pos, Action action, Direction direction, int maxY,
      int sequence, CallbackInfo ci) {
    DeobfuscationEvents.DAMAGE_BLOCK.invoker().onDamageBlock(level, pos);
  }
}
