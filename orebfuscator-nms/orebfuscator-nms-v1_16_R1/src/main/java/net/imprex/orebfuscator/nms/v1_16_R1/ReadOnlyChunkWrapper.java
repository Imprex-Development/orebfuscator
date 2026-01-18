package net.imprex.orebfuscator.nms.v1_16_R1;

import dev.imprex.orebfuscator.interop.ChunkAccessor;
import net.minecraft.server.v1_16_R1.Chunk;

public class ReadOnlyChunkWrapper implements ChunkAccessor {

  private final Chunk chunk;

  ReadOnlyChunkWrapper(Chunk chunk) {
    this.chunk = chunk;
  }

  @Override
  public int getBlockState(int x, int y, int z) {
    return NmsManager.getBlockState(chunk, x, y, z);
  }
}
