package dev.imprex.orebfuscator.interop;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface ChunkAccessor {

  ChunkAccessor EMPTY = (x, y, z) -> 0;

  static long chunkCoordsToLong(int chunkX, int chunkZ) {
    return (chunkZ & 0xffffffffL) << 32 | chunkX & 0xffffffffL;
  }

  @Contract("null -> true; !null -> _")
  static boolean isNullOrEmpty(@Nullable ChunkAccessor chunkAccessor) {
    return chunkAccessor == null || chunkAccessor == EMPTY;
  }

  @Contract("null -> !null; !null -> param1")
  static ChunkAccessor ofNullable(@Nullable ChunkAccessor chunkAccessor) {
    return chunkAccessor == null ? EMPTY : chunkAccessor;
  }

  int getBlockState(int x, int y, int z);
}
