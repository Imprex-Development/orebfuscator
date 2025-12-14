package dev.imprex.orebfuscator.util;

import dev.imprex.orebfuscator.interop.WorldAccessor;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record ChunkCacheKey(String world, int x, int z) {

  public ChunkCacheKey(WorldAccessor world, BlockPos position) {
    this(world.getName(), position.x() >> 4, position.z() >> 4);
  }

  public ChunkCacheKey(WorldAccessor world, int x, int z) {
    this(world.getName(), x, z);
  }

  @Override
  public String toString() {
    return "[%s, (%s, %s)]".formatted(world, x, z);
  }
}
