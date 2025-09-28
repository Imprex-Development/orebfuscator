package net.imprex.orebfuscator.nms.v1_21_R5;

import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.minecraft.world.level.chunk.LevelChunk;

public class ReadOnlyChunkWrapper implements ReadOnlyChunk {

  private final LevelChunk chunk;

  ReadOnlyChunkWrapper(LevelChunk chunk) {
    this.chunk = chunk;
  }

  @Override
  public int getBlockState(int x, int y, int z) {
    return NmsManager.getBlockState(chunk, x, y, z);//ClientboundSetPlayerTeamPacket
  }
}
