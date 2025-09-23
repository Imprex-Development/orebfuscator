package dev.imprex.orebfuscator.interop;

import java.util.concurrent.CompletableFuture;
import dev.imprex.orebfuscator.util.BlockPos;

public interface WorldAccessor {

  String getName();

  int getHeight();

  int getMinBuildHeight();

  int getMaxBuildHeight();

  int getSectionCount();

  int getMinSection();

  int getMaxSection();

  int getSectionIndex(int y);
  
  CompletableFuture<ChunkAccessor[]> getNeighboringChunks(int chunkX, int chunkZ);

  ChunkAccessor getChunk(int chunkX, int chunkZ);

  void sendBlockUpdates(Iterable<BlockPos> iterable);
}
