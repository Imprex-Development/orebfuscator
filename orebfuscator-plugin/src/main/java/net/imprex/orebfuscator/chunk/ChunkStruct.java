package net.imprex.orebfuscator.chunk;

import org.bukkit.World;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;

public class ChunkStruct {

	public static ChunkStruct from(PacketContainer packet, World world) {
		StructureModifier<Integer> packetInteger = packet.getIntegers();
		StructureModifier<byte[]> packetByteArray = packet.getByteArrays();

		ChunkStruct chunkStruct = new ChunkStruct();
		chunkStruct.chunkX = packetInteger.read(0);
		chunkStruct.chunkZ = packetInteger.read(1);
		chunkStruct.primaryBitMask = packetInteger.read(2);
		chunkStruct.data = packetByteArray.read(0);
		chunkStruct.isOverworld = world.getEnvironment() == World.Environment.NORMAL;

		return chunkStruct;
	}

	public int chunkX;
	public int chunkZ;

	public boolean isOverworld;

	public int primaryBitMask;
	public byte[] data;

	public boolean isEmpty() {
		return this.primaryBitMask == 0;
	}
}
