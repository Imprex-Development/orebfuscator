package net.imprex.orebfuscator.chunk;

import org.bukkit.World;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;

public class ChunkStruct {

	public final World world;
	public final int chunkX;
	public final int chunkZ;

	public final boolean isOverworld;

	public final int primaryBitMask;
	public final byte[] data;

	public ChunkStruct(PacketContainer packet, World world) {
		StructureModifier<Integer> packetInteger = packet.getIntegers();
		StructureModifier<byte[]> packetByteArray = packet.getByteArrays();

		this.world = world;
		this.chunkX = packetInteger.read(0);
		this.chunkZ = packetInteger.read(1);
		this.primaryBitMask = packetInteger.read(2);
		this.data = packetByteArray.read(0);
		this.isOverworld = world.getEnvironment() == World.Environment.NORMAL;
	}

	public boolean isEmpty() {
		return this.primaryBitMask == 0;
	}	
}
