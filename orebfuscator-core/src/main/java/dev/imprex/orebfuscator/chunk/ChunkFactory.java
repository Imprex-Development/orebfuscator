package dev.imprex.orebfuscator.chunk;

import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.interop.ServerAccessor;
import dev.imprex.orebfuscator.obfuscation.ObfuscationRequest;

public class ChunkFactory {

  private final RegistryAccessor registryAccessor;
  private final ChunkVersionFlags versionFlags;

  public ChunkFactory(ServerAccessor serverAccessor) {
    this.registryAccessor = serverAccessor.registry();
    this.versionFlags = new ChunkVersionFlags(serverAccessor);
  }

  RegistryAccessor registryAccessor() {
    return registryAccessor;
  }

  ChunkVersionFlags versionFlags() {
    return versionFlags;
  }

  public Chunk fromPacket(ObfuscationRequest request) {
    return new Chunk(this, request);
  }
}
