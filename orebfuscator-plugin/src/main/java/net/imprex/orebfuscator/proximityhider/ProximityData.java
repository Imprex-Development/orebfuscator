package net.imprex.orebfuscator.proximityhider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.World;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.util.BlockCoords;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ProximityData {

	private final Map<Long, Set<BlockCoords>> chunks = new ConcurrentHashMap<>();
	private final World world;

	public ProximityData(World world) {
		this.world = world;
	}

	public ProximityData(Player player) {
		this.world = player.getWorld();
	}

	public World getWorld() {
		return this.world;
	}

	public void addChunk(int chunkX, int chunkZ, Set<BlockCoords> blocks) {
		long key = ChunkPosition.toLong(chunkX, chunkZ);
		this.chunks.computeIfAbsent(key, k -> new HashSet<>()).addAll(blocks);
	}

	public Set<BlockCoords> getChunk(int chunkX, int chunkZ) {
		long key = ChunkPosition.toLong(chunkX, chunkZ);
		return this.chunks.get(key);
	}

	public void removeChunk(int chunkX, int chunkZ) {
		long key = ChunkPosition.toLong(chunkX, chunkZ);
		this.chunks.remove(key);
	}
}