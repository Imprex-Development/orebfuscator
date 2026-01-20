package dev.imprex.orebfuscator.util;

import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.obfuscation.ObfuscationRequest;

@NullMarked
public record ChunkCacheKey(String world, int x, int z) {

  public ChunkCacheKey(ObfuscationRequest request) {
    this(request.world(), request.packet().chunkX(), request.packet().chunkZ());
  }

  public ChunkCacheKey(WorldAccessor world, BlockPos position) {
    this(world.name(), position.x() >> 4, position.z() >> 4);
  }

  public ChunkCacheKey(WorldAccessor world, int x, int z) {
    this(world.name(), x, z);
  }

  @Override
  public String toString() {
    return "[%s, (%s, %s)]".formatted(world, x, z);
  }
}
