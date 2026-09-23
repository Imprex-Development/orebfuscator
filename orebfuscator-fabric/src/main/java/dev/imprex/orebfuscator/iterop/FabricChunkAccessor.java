package dev.imprex.orebfuscator.iterop;

import dev.imprex.orebfuscator.interop.ChunkAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public record FabricChunkAccessor(ChunkAccess chunk) implements ChunkAccessor {

  public int getBlockState(int x, int y, int z) {
    return FabricWorldAccessor.getBlockState(chunk, x, y, z);
  }
}
