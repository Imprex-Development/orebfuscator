package dev.imprex.orebfuscator.proximity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import dev.imprex.orebfuscator.config.api.AdvancedConfig;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.interop.PlayerAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.statistics.ObfuscationStatistics;
import dev.imprex.orebfuscator.util.concurrent.OrebfuscatorExecutor;

public class ProximitySystem implements Runnable {

  private final OrebfuscatorCore orebfuscator;
  private final ObfuscationStatistics statistics;
  private final OrebfuscatorExecutor executor;

  private final int workerCount;
  private final int defaultBucketSize;
  private final long checkInterval;

  private final ProximityWorker worker;

  public ProximitySystem(OrebfuscatorCore orebfuscator) {
    this.orebfuscator = orebfuscator;
    this.statistics = orebfuscator.statistics().obfuscation;
    this.executor = orebfuscator.executor();

    AdvancedConfig advancedConfig = orebfuscator.config().advanced();
    this.workerCount = advancedConfig.threads();
    this.defaultBucketSize = advancedConfig.proximityDefaultBucketSize();
    this.checkInterval = TimeUnit.MILLISECONDS.toNanos(advancedConfig.proximityThreadCheckInterval());

    this.worker = new ProximityWorker(orebfuscator);
  }

  public void start() {
    this.executor.schedule(this, this.checkInterval, TimeUnit.NANOSECONDS);
  }

  @Override
  public void run() {
    long processStart = System.nanoTime();
    process().whenComplete((v, throwable) -> {
      if (throwable != null) {
        OfcLogger.error("An error occurred while running proximity worker", throwable);
      }

      if (this.executor.isShutdown()) {
        return;
      }

      long processTime = System.nanoTime() - processStart;
      this.statistics.proximityProcess.add(processTime);

      // check if we have enough time to sleep
      long waitTime = Math.max(0, this.checkInterval - processTime);
      long waitMillis = TimeUnit.NANOSECONDS.toMillis(waitTime);

      if (waitMillis > 0) {
        // measure wait time
        this.statistics.proximityWait.add(TimeUnit.MILLISECONDS.toNanos(waitMillis));
        this.executor.schedule(this, waitMillis, TimeUnit.MILLISECONDS);
      } else {
        this.executor.execute(this);
      }
    });
  }

  private CompletableFuture<Void> process() {
    var players = this.orebfuscator.players();
    if (players.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    // get player count and derive max bucket size for each thread
    int playerCount = players.size();
    int maxBucketSize = Math.max(this.defaultBucketSize, (int) Math.ceil((float) playerCount / this.workerCount));

    // calculate bucket
    int bucketCount = (int) Math.ceil((float) playerCount / maxBucketSize);
    int bucketSize = (int) Math.ceil((float) playerCount / (float) bucketCount);

    var pendingFutures = new CompletableFuture[bucketCount];
    Iterator<PlayerAccessor> iterator = players.iterator();

    // create buckets and fill queue
    for (int index = 0; index < bucketCount; index++) {
      List<PlayerAccessor> bucket = new ArrayList<>();

      // fill bucket until bucket full or no players remain
      for (int size = 0; size < bucketSize && iterator.hasNext(); size++) {
        bucket.add(iterator.next());
      }

      pendingFutures[index] = CompletableFuture.runAsync(() -> this.worker.process(bucket), this.executor);
    }

    return CompletableFuture.allOf(pendingFutures);
  }
}
