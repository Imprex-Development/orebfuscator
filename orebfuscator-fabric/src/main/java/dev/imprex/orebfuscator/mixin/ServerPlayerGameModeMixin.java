package dev.imprex.orebfuscator.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import dev.imprex.orebfuscator.event.DeobfuscationEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

  @Shadow
  protected ServerLevel level;

  @Definition(id = "action", local = @Local(type = Action.class))
  @Definition(id = "START_DESTROY_BLOCK",
      field = "Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;START_DESTROY_BLOCK:Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;")
  @Expression("action == START_DESTROY_BLOCK")
  @Inject(method = "handleBlockBreakAction", at = @At("MIXINEXTRAS:EXPRESSION"))
  public void orebfuscator$handleBlockBreakAction(BlockPos pos, Action action, Direction direction, int i, int j,
      CallbackInfo ci) {
    DeobfuscationEvents.BREAK_BLOCK.invoker().onBlockBreak(level, pos, action);
  }
}
