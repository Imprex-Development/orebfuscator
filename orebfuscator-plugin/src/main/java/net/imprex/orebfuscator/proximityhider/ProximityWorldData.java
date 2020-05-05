package net.imprex.orebfuscator.proximityhider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.World;

import net.imprex.orebfuscator.util.BlockCoords;

public class ProximityWorldData {

	public static long getKey(int chunkX, int chunkZ) {
		return (chunkZ & 0xffffffffL) << 32 | chunkX & 0xffffffffL;
	}

	private final World world;
	private final Map<Long, Set<BlockCoords>> chunks = new ConcurrentHashMap<>();

	public ProximityWorldData(World world) {
		this.world = world;
	}

	public World getWorld() {
		return this.world;
	}

	public void putBlocks(int chunkX, int chunkZ, Set<BlockCoords> blocks) {
		long key = ProximityWorldData.getKey(chunkX, chunkZ);
		this.chunks.put(key, new HashSet<>(blocks));
	}

	public Set<BlockCoords> getBlocks(int chunkX, int chunkZ) {
		long key = ProximityWorldData.getKey(chunkX, chunkZ);
		return this.chunks.get(key);
	}

	public void removeChunk(int chunkX, int chunkZ) {
		long key = ProximityWorldData.getKey(chunkX, chunkZ);
		this.chunks.remove(key);
	}
}