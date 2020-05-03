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

	private final LoadingCache<Player, ProximityWorldData> playerData = CacheBuilder.newBuilder()
			.build(new CacheLoader<Player, ProximityWorldData>() {

				@Override
				public ProximityWorldData load(Player player) throws Exception {
					return new ProximityWorldData(player.getWorld());
				}
			});

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
				thread.setName("OFC - ProximityHider Thread - #" + i);
				thread.start();
				this.queueThreads[i] = thread;
			}
		}
	}

	public Player pollPlayer() {
		return this.queue.poll();
	}

	public void queuePlayer(Player player) {
		this.queue.offerAndLock(player);
	}

	public void unlockPlayer(Player player) {
		this.queue.unlock(player);
	}

	public void removePlayer(Player player) {
		this.queue.remove(player);
		this.playerData.invalidate(player);
	}

	public ProximityWorldData getPlayer(Player player) {
		try {
			ProximityWorldData proximityWorldData = playerData.get(player);

			if (proximityWorldData.getWorld() != player.getWorld()) {
				proximityWorldData = new ProximityWorldData(player.getWorld());
				playerData.put(player, proximityWorldData);
			}

			return proximityWorldData;
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void addProximityBlocks(Player player, World world, int chunkX, int chunkZ, Set<BlockCoords> blocks) {
		ProximityConfig proximityConfig = this.config.proximity(player.getWorld());

		if (proximityConfig == null || !proximityConfig.enabled()) {
			return;
		}

		ProximityWorldData worldData = this.getPlayer(player);

		if (blocks.size() > 0) {
			worldData.putBlocks(chunkX, chunkZ, blocks);
		} else {
			worldData.removeChunk(chunkX, chunkZ);
		}

		this.queuePlayer(player);
	}

	// TODO needs testing on teleport since I don't know if teleports get chunkunload packets
	public void removeProximityChunks(Player player, World world, int chunkX, int chunkZ) {
		ProximityConfig proximityConfig = this.config.proximity(player.getWorld());

		if (proximityConfig == null || !proximityConfig.enabled()) {
			return;
		}

		this.getPlayer(player).removeChunk(chunkX, chunkZ);
	}

	public void destroy() {
		if (!this.running.compareAndSet(true, false)) {
			throw new IllegalStateException("proximity hider isn't running");
		}

		for (ProximityThread thread : this.queueThreads) {
			if (thread != null) {
				thread.destroy();
			}
		}
	}
}