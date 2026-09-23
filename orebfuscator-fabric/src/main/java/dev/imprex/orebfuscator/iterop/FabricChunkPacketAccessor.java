package dev.imprex.orebfuscator.iterop;

import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.obfuscation.ObfuscationResponse;
import dev.imprex.orebfuscator.util.WrappedClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class FabricChunkPacketAccessor implements ChunkPacketAccessor {

  private final int chunkX;
  private final int chunkZ;

  private final byte[] data;

  private final WrappedClientboundLevelChunkPacketData chunkData;

  public FabricChunkPacketAccessor(ClientboundLevelChunkWithLightPacket packet) {
    this.chunkX = packet.x();
    this.chunkZ = packet.z();

    this.chunkData = new WrappedClientboundLevelChunkPacketData(packet);
    this.data = this.chunkData.getBuffer();
  }

  @Override
  public int chunkX() {
    return this.chunkX;
  }

  @Override
  public int chunkZ() {
    return this.chunkZ;
  }

  @Override
  public boolean isSectionPresent(int i) {
    return true;
  }

  @Override
  public byte[] data() {
    return this.data;
  }

  @Override
  public void update(ObfuscationResponse response) {
    this.chunkData.setBuffer(response.data());
    this.chunkData.removeBlockEntityIf(
        relativePostion -> response.blockEntities().contains(relativePostion.add(chunkX << 4, 0, chunkZ << 4)));
  }
}
