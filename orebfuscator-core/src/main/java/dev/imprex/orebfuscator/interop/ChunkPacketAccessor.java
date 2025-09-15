package dev.imprex.orebfuscator.interop;

import java.util.function.Predicate;
import dev.imprex.orebfuscator.util.BlockPos;

public interface ChunkPacketAccessor {

  WorldAccessor world();

  int chunkX();

  int chunkZ();

  boolean isSectionPresent(int index);

  byte[] data();

  void setData(byte[] data);

  void filterBlockEntities(Predicate<BlockPos> predicate);
}
