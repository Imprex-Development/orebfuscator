package dev.imprex.orebfuscator.util;

import dev.imprex.orebfuscator.reflect.Reflector;
import dev.imprex.orebfuscator.reflect.accessor.FieldAccessor;
import dev.imprex.orebfuscator.reflect.predicate.FieldPredicate;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;

public class WrappedClientboundLevelChunkPacketData {

  private static final FieldAccessor BUFFER =
      Reflector.of(ClientboundLevelChunkPacketData.class).field().type().is(byte[].class).firstOrThrow();
  private static final FieldAccessor BLOCK_ENTITIES =
      Reflector.of(ClientboundLevelChunkPacketData.class).field().type().is(List.class).firstOrThrow();

  private static final Class<?> BLOCK_ENTITY_INFO = ClientboundLevelChunkPacketData.class.getNestMembers()[1];
  private static final FieldPredicate FIELDS = Reflector.of(BLOCK_ENTITY_INFO).field();
  private static final FieldAccessor PACKED_XZ = FIELDS.type().is(byte.class).firstOrThrow();
  private static final FieldAccessor Y = FIELDS.type().is(short.class).firstOrThrow();

  public static void init() {
  }

  private final ClientboundLevelChunkPacketData chunkData;

  public WrappedClientboundLevelChunkPacketData(ClientboundLevelChunkWithLightPacket packet) {
    this.chunkData = packet.chunkData();
  }

  public byte[] getBuffer() {
    return (byte[]) BUFFER.get(this.chunkData);
  }

  public void setBuffer(byte[] buffer) {
    BUFFER.set(this.chunkData, buffer);
  }

  public void removeBlockEntityIf(Predicate<BlockPos> predicate) {
    List<?> blockEntities = (List<?>) BLOCK_ENTITIES.get(this.chunkData);
    for (Iterator<?> iterator = blockEntities.iterator(); iterator.hasNext(); ) {
      Object blockEntityInfo = iterator.next();
      int packedXZ = (byte) PACKED_XZ.get(blockEntityInfo);

      int x = (packedXZ >> 4) & 15;
      int y = (short) Y.get(blockEntityInfo);
      int z = packedXZ & 15;

      if (predicate.test(new BlockPos(x, y, z))) {
        iterator.remove();
      }
    }
  }
}
