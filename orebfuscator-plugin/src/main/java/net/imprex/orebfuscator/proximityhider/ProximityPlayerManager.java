package net.imprex.orebfuscator.proximityhider;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.bukkit.entity.Player;

import net.imprex.orebfuscator.util.BlockPos;

public class ProximityPlayerManager {

	private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final Map<Player, ProximityPlayer> proximityData = new WeakHashMap<>();

	private final ProximityHider proximityHider;

	public ProximityPlayerManager(ProximityHider proximityHider) {
		this.proximityHider = proximityHider;
	}

	private ProximityPlayer getOrCreate(Player player) {
		this.lock.readLock().lock();
		try {
			ProximityPlayer data = this.proximityData.get(player);
			if (data != null) {
				return data;
			}
		} finally {
			this.lock.readLock().unlock();
		}

		ProximityPlayer data = new ProximityPlayer(player);
		this.lock.writeLock().lock();
		try {
			this.proximityData.putIfAbsent(player, data);
			return this.proximityData.get(player);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	ProximityPlayer get(Player player) {
		ProximityPlayer data = this.getOrCreate(player);
		data.setWorld(player.getWorld());
		return data;
	}

	public void remove(Player player) {
		this.lock.writeLock().lock();
		try {
			this.proximityData.remove(player);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	public void addAndLockChunk(Player player, int chunkX, int chunkZ, Set<BlockPos> blocks) {
		if (this.proximityHider.isInProximityWorld(player)) {
			ProximityPlayer data = this.get(player);
			if (blocks.isEmpty()) {
				data.removeChunk(chunkX, chunkZ);
			} else {
				data.addAndLockChunk(chunkX, chunkZ, blocks);
			}
		}
	}

	public void addChunk(Player player, int chunkX, int chunkZ, Set<BlockPos> blocks) {
		if (this.proximityHider.isInProximityWorld(player)) {
			ProximityPlayer data = this.get(player);
			if (blocks.isEmpty()) {
				data.removeChunk(chunkX, chunkZ);
			} else {
				data.addChunk(chunkX, chunkZ, blocks);
			}
		}
	}

	public void unlockChunk(Player player, int chunkX, int chunkZ) {
		if (this.proximityHider.isInProximityWorld(player)) {
			this.get(player).unlockChunk(chunkX, chunkZ);
		}
	}

	public void removeChunk(Player player, int chunkX, int chunkZ) {
		if (this.proximityHider.isInProximityWorld(player)) {
			this.get(player).removeChunk(chunkX, chunkZ);
		}
	}

	public void clear() {
		this.lock.writeLock().lock();
		try {
			this.proximityData.clear();
		} finally {
			this.lock.writeLock().unlock();
		}
	}
}
