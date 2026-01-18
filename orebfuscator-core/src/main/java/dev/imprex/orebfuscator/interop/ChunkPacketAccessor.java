package dev.imprex.orebfuscator.interop;

import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.obfuscation.ObfuscationResponse;

@NullMarked
public interface ChunkPacketAccessor {

  int chunkX();

  int chunkZ();

  boolean isSectionPresent(int index);

  byte[] data();

  void update(ObfuscationResponse response);

}
