package dev.imprex.orebfuscator.obfuscation;

import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.interop.PlayerAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.util.ChunkDirection;

@NullMarked
public record ObfuscationRequest(
    WorldAccessor world,
    PlayerAccessor player,
    ChunkPacketAccessor packet,
    ChunkAccessor @Nullable [] neighborChunks) {

  @SuppressWarnings("ConstantConditions")
  public ObfuscationRequest {
    Objects.requireNonNull(world);
    Objects.requireNonNull(player);
    Objects.requireNonNull(packet);

    if (neighborChunks != null && neighborChunks.length != 4) {
      throw new IllegalArgumentException("Expected 4 neighboring chunks but got " + neighborChunks.length);
    } else if (neighborChunks != null && Arrays.stream(neighborChunks).anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Neighboring chunks must not be null");
    }
  }

  public ObfuscationRequest withNeighbors(ChunkAccessor[] neighborChunks) {
    return new ObfuscationRequest(world, player, packet, Objects.requireNonNull(neighborChunks));
  }

  public int getBlockState(int x, int y, int z) {
    if (neighborChunks != null) {
      ChunkDirection direction = ChunkDirection.fromPosition(packet, x, z);
      return neighborChunks[direction.ordinal()].getBlockState(x, y, z);
    }
    return 0;
  }
}
