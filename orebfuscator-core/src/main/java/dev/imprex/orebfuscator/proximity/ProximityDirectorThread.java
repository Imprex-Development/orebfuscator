package dev.imprex.orebfuscator.proximity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import dev.imprex.orebfuscator.config.api.AdvancedConfig;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.interop.PlayerAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.statistics.ObfuscationStatistics;

// TODO: rewrite to use new OrebfuscatorExecutor
public class ProximityDirectorThread extends Thread {

  private final OrebfuscatorCore orebfuscator;
  private final ObfuscationStatistics statistics;

  private final int workerCount;
  private final int defaultBucketSize;
  private final long checkInterval;

  private final Phaser phaser = new Phaser(1);
  private volatile boolean running = true;

  private final ProximityWorker worker;
  private final ProximityWorkerThread[] workerThreads;

  private final BlockingQueue<List<PlayerAccessor>> bucketQueue = new LinkedBlockingQueue<>();

  public ProximityDirectorThread(OrebfuscatorCore orebfuscator) {
    super(OrebfuscatorCore.THREAD_GROUP, "orebfuscator-proximity-director");
    this.setDaemon(true);

    this.orebfuscator = orebfuscator;
    this.statistics = orebfuscator.statistics().obfuscation;

    AdvancedConfig advancedConfig = orebfuscator.config().advanced();
    this.workerCount = advancedConfig.proximityThreads();
    this.defaultBucketSize = advancedConfig.proximityDefaultBucketSize();
    this.checkInterval = TimeUnit.MILLISECONDS.toNanos(advancedConfig.proximityThreadCheckInterval());

    this.worker = new ProximityWorker(orebfuscator);
    this.workerThreads = new ProximityWorkerThread[workerCount - 1];
  }

  @Override
  public void start() {
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

  List<PlayerAccessor> nextBucket() throws InterruptedException {
    return this.bucketQueue.take();
  }

  void finishBucketProcessing() {
    this.phaser.arriveAndDeregister();
  }

  @Override
  public void run() {
    while (this.isRunning()) {
      try {
        long processStart = System.nanoTime();
        List<PlayerAccessor> players = this.orebfuscator.getPlayers();

        // park thread if no players are online
        if (players.isEmpty()) {
          // park for 1sec and retry
          LockSupport.parkNanos(this, 1000000000L);
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
        List<PlayerAccessor> localBucket = null;

        Iterator<PlayerAccessor> iterator = players.iterator();

        // create buckets and fill queue
        for (int index = 0; index < bucketCount; index++) {
          List<PlayerAccessor> bucket = new ArrayList<>();

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
        if (localBucket != null) {
          this.worker.process(localBucket);
        }

        // wait for all threads to finish and reset phaser
        this.phaser.awaitAdvanceInterruptibly(this.phaser.arrive());

        long processTime = System.nanoTime() - processStart;
        this.statistics.proximityProcess.add(processTime);

        // check if we have enough time to sleep
        long waitTime = Math.max(0, this.checkInterval - processTime);
        long waitMillis = TimeUnit.NANOSECONDS.toMillis(waitTime);

        if (waitMillis > 0) {
          // measure wait time
          this.statistics.proximityWait.add(TimeUnit.MILLISECONDS.toNanos(waitMillis));

          Thread.sleep(waitMillis);
        }
      } catch (InterruptedException e) {
        continue;
      } catch (Exception e) {
        OfcLogger.error(e);
      }
    }

    if (this.phaser.isTerminated() && this.running) {
      OfcLogger.error("Looks like we encountered an invalid state, please report this:",
          new IllegalStateException("Proximity Phaser terminated!"));
    }
  }
}
