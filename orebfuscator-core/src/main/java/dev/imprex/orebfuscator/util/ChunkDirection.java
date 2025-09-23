package dev.imprex.orebfuscator.util;

import org.jetbrains.annotations.NotNull;
import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;

public enum ChunkDirection {

  NORTH(1, 0), EAST(0, 1), SOUTH(-1, 0), WEST(0, -1);

  private final int offsetX;
  private final int offsetZ;

  ChunkDirection(int offsetX, int offsetZ) {
    this.offsetX = offsetX;
    this.offsetZ = offsetZ;
  }

  public int getOffsetX() {
    return offsetX;
  }

  public int getOffsetZ() {
    return offsetZ;
  }

  public static ChunkDirection fromPosition(@NotNull ChunkPacketAccessor packetAccessor, int targetX, int targetZ) {
    int offsetX = (targetX >> 4) - packetAccessor.chunkX();
    int offsetZ = (targetZ >> 4) - packetAccessor.chunkZ();

    if (offsetX == 1 && offsetZ == 0) {
      return NORTH;
    } else if (offsetX == 0 && offsetZ == 1) {
      return EAST;
    } else if (offsetX == -1 && offsetZ == 0) {
      return SOUTH;
    } else if (offsetX == 0 && offsetZ == -1) {
      return WEST;
    }

    throw new IllegalArgumentException(String.format("invalid offset (chunkX: %d, chunkZ: %d, x: %d, z: %d)",
        packetAccessor.chunkX(), packetAccessor.chunkZ(), targetX, targetZ));
  }

  @Deprecated
  public static ChunkDirection fromPosition(ChunkCacheKey key, int targetX, int targetZ) {
    int offsetX = (targetX >> 4) - key.x();
    int offsetZ = (targetZ >> 4) - key.z();

    if (offsetX == 1 && offsetZ == 0) {
      return NORTH;
    } else if (offsetX == 0 && offsetZ == 1) {
      return EAST;
    } else if (offsetX == -1 && offsetZ == 0) {
      return SOUTH;
    } else if (offsetX == 0 && offsetZ == -1) {
      return WEST;
    }

    throw new IllegalArgumentException(
        String.format("invalid offset (origin: %s, x: %d, z: %d)", key, targetX, targetZ));
  }
}
