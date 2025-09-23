package dev.imprex.orebfuscator.interop;

public interface ChunkAccessor {

  ChunkAccessor EMPTY = (x, y, z) -> 0;

  int getBlockState(int x, int y, int z);
}
