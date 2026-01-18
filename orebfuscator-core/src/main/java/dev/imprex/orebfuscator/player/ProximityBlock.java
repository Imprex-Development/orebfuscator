package dev.imprex.orebfuscator.player;

import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.util.BlockPos;

@NullMarked
public record ProximityBlock(BlockPos blockPos, boolean lavaObfuscated) {

  private static final byte FLAG_LAVA_OBFUSCATED = 0x01;
  
  public ProximityBlock(BlockPos blockPos, byte flags) {
    this(blockPos, (flags & FLAG_LAVA_OBFUSCATED) == FLAG_LAVA_OBFUSCATED);
  }
  
  public byte flags() {
    return lavaObfuscated ? FLAG_LAVA_OBFUSCATED : 0x00;
  }
}
