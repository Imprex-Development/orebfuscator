package net.imprex.orebfuscator.nms.v1_18_R1;

import dev.imprex.orebfuscator.interop.ChunkAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public record DefaultChunkAccessor(ChunkAccess chunk) implements ChunkAccessor {

  @Override
  public int getBlockState(int x, int y, int z) {
    return NmsManager.getBlockState(chunk, x, y, z);
  }
}
