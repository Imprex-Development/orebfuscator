package net.imprex.orebfuscator.iterop;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;

import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.obfuscation.ObfuscationResponse;
import dev.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.WrappedClientboundLevelChunkPacketData;

public class BukkitChunkPacketAccessor implements ChunkPacketAccessor {

	private static final boolean HAS_CLIENTBOUND_LEVEL_CHUNK_PACKET_DATA = MinecraftVersion.isAtOrAbove("1.18");
	private static final boolean HAS_HEIGHT_BITMASK = MinecraftVersion.isBelow("1.18");
	private static final boolean HAS_VARINT_BITMASK = MinecraftVersion.isBelow("1.17");

	public final BukkitWorldAccessor worldAccessor;

	private final int chunkX;
	private final int chunkZ;

	private final BitSet sectionMask;
	private final byte[] data;

	private final PacketContainer packet;
	private final WrappedClientboundLevelChunkPacketData packetData;

	public BukkitChunkPacketAccessor(PacketContainer packet, BukkitWorldAccessor worldAccessor) {
		this.packet = packet;
		this.worldAccessor = worldAccessor;

		StructureModifier<Integer> packetInteger = packet.getIntegers();
		this.chunkX = packetInteger.read(0);
		this.chunkZ = packetInteger.read(1);

		if (HAS_CLIENTBOUND_LEVEL_CHUNK_PACKET_DATA) {
			this.packetData = new WrappedClientboundLevelChunkPacketData(packet);
			this.data = this.packetData.getBuffer();
		} else {
			this.packetData = null;
			this.data = packet.getByteArrays().read(0);
		}

		if (HAS_HEIGHT_BITMASK) {
			if (HAS_VARINT_BITMASK) {
				this.sectionMask = convertIntToBitSet(packetInteger.read(2));
			} else {
				this.sectionMask = packet.getSpecificModifier(BitSet.class).read(0);
			}
		} else {
			this.sectionMask = new BitSet();
			this.sectionMask.set(0, worldAccessor.getSectionCount());
		}
	}

	@Override
	public WorldAccessor world() {
		return this.worldAccessor;
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
	public boolean isSectionPresent(int index) {
		return this.sectionMask.get(index);
	}

	@Override
	public byte[] data() {
		return this.data;
	}

	@Override
	public void update(ObfuscationResponse response) {
		if (this.packetData != null) {
			this.packetData.setBuffer(response.data());
			this.packetData.removeBlockEntityIf(relativePostion -> response.blockEntities()
					.contains(relativePostion.add(chunkX << 4, 0, chunkZ << 4)));
		} else {
			this.packet.getByteArrays().write(0, response.data());
			removeBlockEntityIf(this.packet, response.blockEntities()::contains);
		}
	}

	private void removeBlockEntityIf(PacketContainer packet, Predicate<BlockPos> predicate) {
		StructureModifier<List<NbtBase<?>>> packetNbtList = packet.getListNbtModifier();

		List<NbtBase<?>> tileEntities = packetNbtList.read(0);
		this.removeBlockEntities(tileEntities, predicate);
		packetNbtList.write(0, tileEntities);
	}

	private void removeBlockEntities(List<NbtBase<?>> tileEntities, Predicate<BlockPos> predicate) {
		for (Iterator<NbtBase<?>> iterator = tileEntities.iterator(); iterator.hasNext();) {
			NbtCompound tileEntity = (NbtCompound) iterator.next();

			int x = tileEntity.getInteger("x");
			int y = tileEntity.getInteger("y");
			int z = tileEntity.getInteger("z");

			BlockPos position = new BlockPos(x, y, z);
			if (predicate.test(position)) {
				iterator.remove();
			}
		}
	}

	public boolean isEmpty() {
		return this.sectionMask.isEmpty();
	}

	private static BitSet convertIntToBitSet(int value) {
		BitSet bitSet = new BitSet();
		for (int index = 0; value != 0; index++) {
			if ((value & 1) == 1) {
				bitSet.set(index);
			}
			value >>>= 1;
		}
		return bitSet;
	}
}
