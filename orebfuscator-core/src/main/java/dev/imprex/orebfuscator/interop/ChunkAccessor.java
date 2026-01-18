package dev.imprex.orebfuscator.interop;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ChunkAccessor {

  ChunkAccessor EMPTY = (x, y, z) -> 0;

  static long chunkCoordsToLong(int chunkX, int chunkZ) {
    return (chunkZ & 0xffffffffL) << 32 | chunkX & 0xffffffffL;
  }

  int getBlockState(int x, int y, int z);
}
