package net.imprex.orebfuscator.proximityhider;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.World;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ProximityPlayer {

	private final Map<Long, Set<BlockPos>> chunks = new ConcurrentHashMap<>();
	private final Set<Long> lockedChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private final Lock lock = new ReentrantLock();
	private World world;

	public ProximityPlayer(Player player) {
		this.world = player.getWorld();
	}

	public World getWorld() {
		this.lock.lock();
		try {
			return this.world;
		} finally {
			this.lock.unlock();
		}
	}

	public void setWorld(World world) {
		this.lock.lock();
		try {
			if (this.world != world) {
				this.world = world;
				this.chunks.clear();
				this.lockedChunks.clear();
			}
		} finally {
			this.lock.unlock();
		}
	}

	public void unlockChunk(int chunkX, int chunkZ) {
		this.lockedChunks.remove(ChunkPosition.toLong(chunkX, chunkZ));
	}

	public void addAndLockChunk(int chunkX, int chunkZ, Set<BlockPos> blocks) {
		long key = ChunkPosition.toLong(chunkX, chunkZ);
		this.chunks.computeIfAbsent(key, k -> {
			return Collections.newSetFromMap(new ConcurrentHashMap<>());
		}).addAll(blocks);
		this.lockedChunks.add(key);
	}

	public Set<BlockPos> getChunk(int chunkX, int chunkZ) {
		long key = ChunkPosition.toLong(chunkX, chunkZ);
		if (this.lockedChunks.contains(key)) {
			return null;
		}
		return this.chunks.get(key);
	}

	public void removeChunk(int chunkX, int chunkZ) {
		long key = ChunkPosition.toLong(chunkX, chunkZ);
		this.chunks.remove(key);
		this.lockedChunks.remove(key);
	}
}