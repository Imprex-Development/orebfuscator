package dev.imprex.orebfuscator.interop;

import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.player.OrebfuscatorPlayer;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.EntityPose;

@NullMarked
public interface PlayerAccessor {

  OrebfuscatorPlayer orebfuscatorPlayer();

  EntityPose pose();

  EntityPose eyePose();

  WorldAccessor world();

  boolean isAlive();

  boolean isSpectator();

  double lavaFogDistance();

  boolean hasPermission(PermissionRequirements permission);

  void runForPlayer(Runnable runnable);

  void sendBlockUpdates(Iterable<BlockPos> iterable);
}
