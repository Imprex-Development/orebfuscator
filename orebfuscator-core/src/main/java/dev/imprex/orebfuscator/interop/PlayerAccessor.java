package dev.imprex.orebfuscator.interop;

import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.EntityPose;

public interface PlayerAccessor {

  EntityPose pose();

  EntityPose eyePose();

  WorldAccessor world();

  boolean isAlive();

  boolean isSpectator();

  boolean hasOperatorLevel(int level);

  boolean hasPermission(String permission);

  void runForPlayer(Runnable runnable);

  void sendBlockUpdates(Iterable<BlockPos> iterable);

}
