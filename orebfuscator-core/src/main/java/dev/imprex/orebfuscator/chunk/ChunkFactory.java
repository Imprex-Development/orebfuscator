package dev.imprex.orebfuscator.chunk;

import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.interop.ServerAccessor;

public class ChunkFactory {

  private final RegistryAccessor registryAccessor;
  private final ChunkVersionFlags versionFlags;

  public ChunkFactory(ServerAccessor serverAccessor) {
    this.registryAccessor = serverAccessor.getRegistry();
    this.versionFlags = new ChunkVersionFlags(serverAccessor);
  }

  RegistryAccessor registryAccessor() {
    return registryAccessor;
  }

  ChunkVersionFlags versionFlags() {
    return versionFlags;
  }

  public Chunk fromPacket(ChunkPacketAccessor packet) {
    return new Chunk(this, packet);
  }
}
