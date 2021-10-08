package net.imprex.orebfuscator.proximityhider;

import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;

public class ProximityHider {

	private final Orebfuscator orebfuscator;
	private final OrebfuscatorConfig config;

	private final ProximityPlayerManager dataManager = new ProximityPlayerManager(this);
	private final ProximityQueue queue = new ProximityQueue();

	private final AtomicBoolean running = new AtomicBoolean();
	private final ProximityThread[] queueThreads;

	public ProximityHider(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		this.config = this.orebfuscator.getOrebfuscatorConfig();

		this.queueThreads = new ProximityThread[this.config.general().proximityHiderRunnerSize()];
	}

	public ProximityPlayerManager getPlayerManager() {
		return dataManager;
	}

	ProximityQueue getQueue() {
		return queue;
	}

	public boolean isInProximityWorld(Player player) {
		ProximityConfig proximityConfig = this.config.proximity(player.getWorld());
		return proximityConfig != null && proximityConfig.isEnabled();
	}

	public boolean shouldIgnorePlayer(Player player) {
		return player.getGameMode() == GameMode.SPECTATOR && this.config.general().ignoreSpectator();
	}

	public void queuePlayerUpdate(Player player) {
		if (this.isInProximityWorld(player) && !shouldIgnorePlayer(player)) {
			this.queue.offerAndLock(player);
		}
	}

	public void removePlayer(Player player) {
		this.queue.remove(player);
		this.dataManager.remove(player);
	}

	public void start() {
		if (!this.running.compareAndSet(false, true)) {
			throw new IllegalStateException("proximity hider already running");
		}

		for (int i = 0; i < this.queueThreads.length; i++) {
			ProximityThread thread = new ProximityThread(this, this.orebfuscator);
			thread.setDaemon(true);
			thread.start();
			this.queueThreads[i] = thread;
		}
	}

	public void close() {
		if (!this.running.compareAndSet(true, false)) {
			throw new IllegalStateException("proximity hider isn't running");
		}

		this.queue.clear();
		this.dataManager.clear();

		for (ProximityThread thread : this.queueThreads) {
			if (thread != null) {
				thread.close();
			}
		}
	}
}