package dev.imprex.orebfuscator.interop;

import dev.imprex.orebfuscator.player.OrebfuscatorPlayer;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.EntityPose;

// TODO: nullability
public interface PlayerAccessor {

  OrebfuscatorPlayer orebfuscatorPlayer();

  EntityPose pose();

  EntityPose eyePose();

  WorldAccessor world();

  boolean isAlive();

  boolean isSpectator();
  
  double lavaFogDistance();

  boolean hasPermission(String permission);

  void runForPlayer(Runnable runnable);

  void sendBlockUpdates(Iterable<BlockPos> iterable);
}
