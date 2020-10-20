package net.imprex.orebfuscator.proximityhider;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.util.BlockCoords;

public class ProximityHider {

	private final LoadingCache<Player, ProximityData> playerDataCache = CacheBuilder.newBuilder()
			.build(CacheLoader.from(ProximityData::new));

	private final Orebfuscator orebfuscator;
	private final OrebfuscatorConfig config;

	private final ProximityQueue queue = new ProximityQueue();

	private final AtomicBoolean running = new AtomicBoolean();
	private final ProximityThread[] queueThreads;

	public ProximityHider(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		this.config = this.orebfuscator.getOrebfuscatorConfig();

		this.queueThreads = new ProximityThread[this.config.general().proximityHiderRunnerSize()];
	}

	public void start() {
		if (!this.running.compareAndSet(false, true)) {
			throw new IllegalStateException("proximity hider already running");
		}

		for (int i = 0; i < this.queueThreads.length; i++) {
			if (this.queueThreads[i] == null) {
				ProximityThread thread = new ProximityThread(this, this.orebfuscator);
				thread.setDaemon(true);
				thread.start();
				this.queueThreads[i] = thread;
			}
		}
	}

	ProximityQueue getQueue() {
		return queue;
	}

	public void queuePlayerUpdate(Player player) {
		ProximityConfig proximityConfig = this.config.proximity(player.getWorld());
		if (proximityConfig != null && proximityConfig.enabled()) {
			this.queue.offerAndLock(player);
		}
	}

	public void invalidatePlayer(Player player) {
		this.queue.remove(player);
		this.playerDataCache.invalidate(player);
	}

	public ProximityData getPlayerData(Player player) {
		try {
			ProximityData playerData = this.playerDataCache.get(player);
			if (playerData.getWorld() != player.getWorld()) {
				playerData = new ProximityData(player);
				this.playerDataCache.put(player, playerData);
			}
			return playerData;
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void addProximityBlocks(Player player, int chunkX, int chunkZ, Set<BlockCoords> blocks) {
		ProximityData playerData = this.getPlayerData(player);

		if (blocks.size() > 0) {
			playerData.addChunk(chunkX, chunkZ, blocks);
		} else {
			playerData.removeChunk(chunkX, chunkZ);
		}

		this.queuePlayerUpdate(player);
	}

	public void removeProximityChunks(Player player, World world, int chunkX, int chunkZ) {
		this.getPlayerData(player).removeChunk(chunkX, chunkZ);
	}

	public void destroy() {
		if (!this.running.compareAndSet(true, false)) {
			throw new IllegalStateException("proximity hider isn't running");
		}

		this.queue.clear();
		this.playerDataCache.invalidateAll();

		for (ProximityThread thread : this.queueThreads) {
			if (thread != null) {
				// TODO set thread null
				thread.close();
			}
		}
	}
}