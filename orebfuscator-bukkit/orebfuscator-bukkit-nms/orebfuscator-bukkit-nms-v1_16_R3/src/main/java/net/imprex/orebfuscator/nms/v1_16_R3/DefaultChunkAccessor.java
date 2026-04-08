package net.imprex.orebfuscator.nms.v1_16_R3;

import dev.imprex.orebfuscator.interop.ChunkAccessor;
import net.minecraft.server.v1_16_R3.Chunk;

public record DefaultChunkAccessor(Chunk chunk) implements ChunkAccessor {

  @Override
  public int getBlockState(int x, int y, int z) {
    return NmsManager.getBlockState(chunk, x, y, z);
  }
}
