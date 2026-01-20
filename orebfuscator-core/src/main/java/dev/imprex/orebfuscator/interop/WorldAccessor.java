package dev.imprex.orebfuscator.interop;

import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.config.api.WorldConfigBundle;
import dev.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import dev.imprex.orebfuscator.util.BlockPos;

@NullMarked
public interface WorldAccessor {

  String name();

  int height();

  int minBuildHeight();

  int maxBuildHeight();

  int sectionCount();

  int minSection();

  int maxSection();

  int sectionIndex(int y);

  WorldConfigBundle config();

  CompletableFuture<ChunkAccessor[]> getNeighboringChunks(ObfuscationRequest request);

  ChunkAccessor getChunk(int chunkX, int chunkZ);

  @Deprecated
  int getBlockState(int x, int y, int z);

  void sendBlockUpdates(Iterable<BlockPos> iterable);
}
