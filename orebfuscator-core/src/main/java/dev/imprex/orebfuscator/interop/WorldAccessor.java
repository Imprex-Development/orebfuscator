package dev.imprex.orebfuscator.interop;

import dev.imprex.orebfuscator.util.ChunkDirection;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.config.api.WorldConfigBundle;
import dev.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import dev.imprex.orebfuscator.util.BlockPos;
import org.jspecify.annotations.Nullable;

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

  ChunkAccessor getChunkNow(int chunkX, int chunkZ);

  void sendBlockUpdates(Iterable<BlockPos> iterable);
}
