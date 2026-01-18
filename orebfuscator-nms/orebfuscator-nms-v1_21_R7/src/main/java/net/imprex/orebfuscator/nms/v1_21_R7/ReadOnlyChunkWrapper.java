package net.imprex.orebfuscator.nms.v1_21_R7;

import dev.imprex.orebfuscator.interop.ChunkAccessor;
import net.minecraft.world.level.chunk.LevelChunk;

public class ReadOnlyChunkWrapper implements ChunkAccessor {

  private final LevelChunk chunk;

  ReadOnlyChunkWrapper(LevelChunk chunk) {
    this.chunk = chunk;
  }

  @Override
  public int getBlockState(int x, int y, int z) {
    return NmsManager.getBlockState(chunk, x, y, z);
  }
}
