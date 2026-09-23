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

  public static final Event<DamageBlock> DAMAGE_BLOCK =
      EventFactory.createArrayBacked(DamageBlock.class, callbacks -> (level, blockPos) -> {
        for (DamageBlock callback : callbacks) {
          callback.onDamageBlock(level, blockPos);
        }
      });

  private DeobfuscationEvents() {
  }

  @FunctionalInterface
  public interface SetBlock {
    void onSetBlock(ServerLevel level, BlockPos blockPos);
  }


  @FunctionalInterface
  public interface DamageBlock {
    void onDamageBlock(ServerLevel level, BlockPos blockPos);
  }
}
