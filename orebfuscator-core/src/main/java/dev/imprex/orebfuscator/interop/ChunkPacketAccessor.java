package dev.imprex.orebfuscator.interop;

import dev.imprex.orebfuscator.obfuscation.ObfuscationResponse;

// TODO: nullability
public interface ChunkPacketAccessor {

  int chunkX();

  int chunkZ();

  boolean isSectionPresent(int index);

  byte[] data();

  void update(ObfuscationResponse response);

}
