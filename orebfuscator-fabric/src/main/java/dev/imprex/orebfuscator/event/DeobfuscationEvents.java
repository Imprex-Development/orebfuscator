package dev.imprex.orebfuscator.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DeobfuscationEvents {

  public static final Event<SetBlock> SET_BLOCK =
      EventFactory.createArrayBacked(SetBlock.class, callbacks -> (level, blockPos) -> {
        for (SetBlock callback : callbacks) {
          callback.onSetBlock(level, blockPos);
        }
      });

  public static final Event<BreakBlock> BREAK_BLOCK =
      EventFactory.createArrayBacked(BreakBlock.class, callbacks -> (level, blockPos, action) -> {
        for (BreakBlock callback : callbacks) {
          callback.onBlockBreak(level, blockPos, action);
        }
      });

  private DeobfuscationEvents() {
  }

  @FunctionalInterface
  public interface SetBlock {
    void onSetBlock(ServerLevel level, BlockPos blockPos);
  }


  @FunctionalInterface
  public interface BreakBlock {
    void onBlockBreak(ServerLevel level, BlockPos blockPos, ServerboundPlayerActionPacket.Action action);
  }
}
