package net.imprex.orebfuscator.util;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;

import dev.imprex.orebfuscator.util.BlockPos;

public class WrappedClientboundLevelChunkPacketData {

	private static final Class<?> CLIENTBOUND_LEVEL_CHUNK_PACKET_DATA = MinecraftReflection.getMinecraftClass("network.protocol.game.ClientboundLevelChunkPacketData");
	private static final FieldAccessor BUFFER = Accessors.getFieldAccessor(CLIENTBOUND_LEVEL_CHUNK_PACKET_DATA, byte[].class, true);
	private static final FieldAccessor BLOCK_ENTITIES = Accessors.getFieldAccessor(CLIENTBOUND_LEVEL_CHUNK_PACKET_DATA, List.class, true);

	private static final Class<?> BLOCK_ENTITY_INFO = MinecraftReflection.getMinecraftClass(
			"network.protocol.game.ClientboundLevelChunkPacketData$BlockEntityInfo",
			"network.protocol.game.ClientboundLevelChunkPacketData$a");
	private static final FieldAccessor[] INT_FIELDS = Accessors.getFieldAccessorArray(BLOCK_ENTITY_INFO, int.class, true);
	private static final FieldAccessor PACKED_XZ = INT_FIELDS[0];
	private static final FieldAccessor Y = INT_FIELDS[1];

	private final Object handle;

	public WrappedClientboundLevelChunkPacketData(PacketContainer packet) {
		this.handle = packet.getSpecificModifier(CLIENTBOUND_LEVEL_CHUNK_PACKET_DATA).read(0);
	}

	public byte[] getBuffer() {
		return (byte[]) BUFFER.get(this.handle);
	}

	public void setBuffer(byte[] buffer) {
		BUFFER.set(this.handle, buffer);
	}

	public void removeBlockEntityIf(Predicate<BlockPos> predicate) {
		List<?> blockEntities = (List<?>) BLOCK_ENTITIES.get(this.handle);
		for (Iterator<?> iterator = blockEntities.iterator(); iterator.hasNext();) {
			Object blockEntityInfo = iterator.next();
			int packedXZ = (int) PACKED_XZ.get(blockEntityInfo);

			int x = (packedXZ >> 4) & 15;
			int y = (int) Y.get(blockEntityInfo);
			int z = packedXZ & 15;

			if (predicate.test(new BlockPos(x, y, z))) {
				iterator.remove();
			}
		}
	}
}
