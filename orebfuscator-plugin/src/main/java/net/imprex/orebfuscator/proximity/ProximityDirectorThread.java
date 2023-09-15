package net.imprex.orebfuscator.proximity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.LockSupport;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.AdvancedConfig;
import net.imprex.orebfuscator.util.OFCLogger;

public class ProximityDirectorThread extends Thread implements Listener {

	private final Orebfuscator orebfuscator;
	private final int workerCount;
	private final int defaultBucketSize;
	private final int checkInterval;

	private final Phaser phaser = new Phaser(1);
	private volatile boolean running = true;

	private final ProximityWorker worker;
	private final ProximityWorkerThread[] workerThreads;

	private final BlockingQueue<List<Player>> bucketQueue = new LinkedBlockingQueue<>();

	public ProximityDirectorThread(Orebfuscator orebfuscator) {
		super(Orebfuscator.THREAD_GROUP, "ofc-proximity-director");
		this.setDaemon(true);

		this.orebfuscator = orebfuscator;

		AdvancedConfig advancedConfig = orebfuscator.getOrebfuscatorConfig().advanced();
		this.workerCount = advancedConfig.proximityHiderThreads();
		this.defaultBucketSize = advancedConfig.proximityDefaultBucketSize();
		this.checkInterval = advancedConfig.proximityThreadCheckInterval();

		this.worker = new ProximityWorker(orebfuscator);
		this.workerThreads = new ProximityWorkerThread[workerCount - 1];
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		if (LockSupport.getBlocker(this) == this) {
			LockSupport.unpark(this);
		}
	}

	@Override
	public void start() {
		Bukkit.getPluginManager().registerEvents(this, this.orebfuscator);

		super.start();

		for (int i = 0; i < workerCount - 1; i++) {
			this.workerThreads[i] = new ProximityWorkerThread(this, this.worker);
			this.workerThreads[i].start();
		}
	}

	public void close() {
		this.running = false;

		this.interrupt();

		for (int i = 0; i < workerCount - 1; i++) {
			this.workerThreads[i].interrupt();
		}

		// technically not need but better be safe
		this.phaser.forceTermination();
	}

	boolean isRunning() {
		return this.running && !this.phaser.isTerminated();
	}

	List<Player> nextBucket() throws InterruptedException {
		return this.bucketQueue.take();
	}

	void finishBucketProcessing() {
		this.phaser.arriveAndDeregister();
	}

	@Override
	public void run() {
		while (this.isRunning()) {
			try {
				Collection<? extends Player> players = Bukkit.getOnlinePlayers();

				// park thread if no players are online
				if (players.isEmpty()) {
					LockSupport.park(this);
					// reset interrupt flag
					Thread.interrupted();
					continue;
				}

				// get player count and derive max bucket size for each thread
				int playerCount = players.size();
				int maxBucketSize = Math.max(this.defaultBucketSize, (int) Math.ceil((float) playerCount / this.workerCount));

				// calculate bucket
				int bucketCount = (int) Math.ceil((float) playerCount / maxBucketSize);
				int bucketSize = (int) Math.ceil((float) playerCount / (float) bucketCount);

				// register extra worker threads in phaser
				if (bucketCount > 1) {
					this.phaser.bulkRegister(bucketCount - 1);
				}

				// this threads local bucket
				List<Player> localBucket = null;

				Iterator<? extends Player> iterator = players.iterator();

				// create buckets and fill queue
				for (int index = 0; index < bucketCount; index++) {
					List<Player> bucket = new ArrayList<>();

					// fill bucket until bucket full or no players remain
					for (int size = 0; size < bucketSize && iterator.hasNext(); size++) {
						bucket.add(iterator.next());
					}

					// assign first bucket to current thread
					if (index == 0) {
						localBucket = bucket;
					} else {
						this.bucketQueue.offer(bucket);
					}
				}

				// process local bucket
				this.worker.process(localBucket);

				// wait for all threads to finish and reset phaser
				this.phaser.awaitAdvanceInterruptibly(this.phaser.arrive());

				// sleep till next execution
				Thread.sleep(this.checkInterval);
			} catch (InterruptedException e) {
				continue;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (this.phaser.isTerminated() && this.running) {
			OFCLogger.error("Looks like we encountered an invalid state, please report this:",
					new IllegalStateException("Proximity Phaser terminated!"));
		}
	}
}
