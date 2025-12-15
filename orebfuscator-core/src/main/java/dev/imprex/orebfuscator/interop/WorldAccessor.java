package dev.imprex.orebfuscator.interop;

import java.util.concurrent.CompletableFuture;
import dev.imprex.orebfuscator.config.api.WorldConfigBundle;
import dev.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import dev.imprex.orebfuscator.util.BlockPos;

// TODO: nullability
public interface WorldAccessor {

  String getName();

  int getHeight();

  int getMinBuildHeight();

  int getMaxBuildHeight();

  int getSectionCount();

  int getMinSection();

  int getMaxSection();

  int getSectionIndex(int y);

  WorldConfigBundle config();

  CompletableFuture<ChunkAccessor[]> getNeighboringChunks(ObfuscationRequest request);

  ChunkAccessor getChunk(int chunkX, int chunkZ);
  
  @Deprecated
  int getBlockState(int x, int y, int z);

  void sendBlockUpdates(Iterable<BlockPos> iterable);
}
