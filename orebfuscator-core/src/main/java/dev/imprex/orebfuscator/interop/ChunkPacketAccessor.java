package dev.imprex.orebfuscator.interop;

import dev.imprex.orebfuscator.obfuscation.ObfuscationResponse;

public interface ChunkPacketAccessor {

  WorldAccessor world();

  int chunkX();

  int chunkZ();

  boolean isSectionPresent(int index);

  byte[] data();

  void update(ObfuscationResponse response);

}
