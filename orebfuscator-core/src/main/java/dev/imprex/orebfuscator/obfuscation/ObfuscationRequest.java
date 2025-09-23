package dev.imprex.orebfuscator.obfuscation;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.ChunkDirection;

public record ObfuscationRequest(
    @NotNull CompletableFuture<ObfuscationResponse> future,
    @NotNull ChunkPacketAccessor packetAccessor,
    @Nullable ChunkAccessor[] neighborChunks) {

  public ObfuscationRequest(
      @NotNull CompletableFuture<ObfuscationResponse> future,
      @NotNull ChunkPacketAccessor packetAccessor,
      @Nullable ChunkAccessor[] neighborChunks) {
    this.future = Objects.requireNonNull(future);
    this.packetAccessor = Objects.requireNonNull(packetAccessor);
    this.neighborChunks = neighborChunks;

    if (neighborChunks != null && neighborChunks.length != 4) {
      throw new IllegalArgumentException("Expected 4 neighboring chunks but got " + neighborChunks.length);
    }
  }

  public ObfuscationRequest(@NotNull ChunkPacketAccessor packetAccessor, @Nullable ChunkAccessor[] neighborChunks) {
    this(new CompletableFuture<ObfuscationResponse>(), packetAccessor, neighborChunks);
  }

  public ObfuscationRequest(@NotNull ObfuscationRequest request, @NotNull ChunkAccessor[] neighborChunks) {
    this(request.future(), request.packetAccessor(), Objects.requireNonNull(neighborChunks));
  }

  public int getBlockState(int x, int y, int z) {
    if (neighborChunks != null) {
      ChunkDirection direction = ChunkDirection.fromPosition(packetAccessor, x, z);
      return neighborChunks[direction.ordinal()].getBlockState(x, y, z);
    }

    return 0;
  }

  public void complete(byte[] data, Set<BlockPos> blockEntities, List<BlockPos> proximityBlocks) {
      future().complete(new ObfuscationResponse(data, blockEntities, proximityBlocks));
  }

  public void completeExceptionally(Throwable throwable) {
    future().completeExceptionally(throwable);
  }
}
