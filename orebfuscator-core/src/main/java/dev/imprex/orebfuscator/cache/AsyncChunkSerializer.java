package dev.imprex.orebfuscator.cache;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.statistics.CacheStatistics;
import dev.imprex.orebfuscator.util.ChunkCacheKey;
import dev.imprex.orebfuscator.util.RollingTimer;

/**
 * This class works similar to a bounded buffer for cache read and write requests but also functions as the only
 * consumer of said buffer. All requests can get reorder similar to modern memory access reordering in CPUs. If for
 * example a write request is already in the buffer and a new read request for the same position is created then the
 * read request doesn't get put in the buffer and gets completed with the content of the write request.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Producer–consumer_problem">Bound buffer</a>
 * @see <a href="https://en.wikipedia.org/wiki/Memory_ordering">Memory ordering</a>
 */
@NullMarked
public class AsyncChunkSerializer implements Runnable {

  private final CacheStatistics statistics;
  
  private final Lock lock = new ReentrantLock(true);
  private final Condition notFull = lock.newCondition();
  private final Condition notEmpty = lock.newCondition();

  private final Map<ChunkCacheKey, Runnable> tasks = new HashMap<>();
  private final Queue<ChunkCacheKey> positions = new LinkedList<>();

  private final int maxTaskQueueSize;
  private final ChunkSerializer serializer;

  private final Thread thread;
  private volatile boolean running = true;

  public AsyncChunkSerializer(OrebfuscatorCore orebfuscator, AbstractRegionFileCache<?> regionFileCache) {
    this.statistics = orebfuscator.statistics().cache;
    this.maxTaskQueueSize = orebfuscator.config().cache().maximumTaskQueueSize();
    this.serializer = new ChunkSerializer(regionFileCache, this.statistics);

    this.thread = new Thread(OrebfuscatorCore.THREAD_GROUP, this, "ofc-chunk-serializer");
    this.thread.setDaemon(true);
    this.thread.start();

    this.statistics.setDiskCacheQueueLength(this.tasks::size);
  }

  public CompletableFuture<@Nullable ChunkCacheEntry> read(ChunkCacheKey key) {
    this.lock.lock();
    try {
      Runnable task = this.tasks.get(key);
      if (task instanceof WriteTask) {
        return CompletableFuture.completedFuture(((WriteTask) task).chunk);
      } else if (task instanceof ReadTask) {
        return ((ReadTask) task).future;
      } else {
        CompletableFuture<ChunkCacheEntry> future = new CompletableFuture<>();
        this.queueTask(key, new ReadTask(key, future));
        return future;
      }
    } finally {
      this.lock.unlock();
    }
  }

  public void write(ChunkCacheKey key, ChunkCacheEntry chunk) {
    this.lock.lock();
    try {
      Runnable prevTask = this.queueTask(key, new WriteTask(key, chunk));
      if (prevTask instanceof ReadTask) {
        ((ReadTask) prevTask).future.complete(chunk);
      }
    } finally {
      this.lock.unlock();
    }
  }

  @Nullable
  private Runnable queueTask(ChunkCacheKey key, Runnable nextTask) {
    while (this.positions.size() >= this.maxTaskQueueSize) {
      this.notFull.awaitUninterruptibly();
    }

    if (!this.running) {
      throw new IllegalStateException("AsyncChunkSerializer already closed");
    }

    Runnable prevTask = this.tasks.put(key, nextTask);
    if (prevTask == null) {
      this.positions.offer(key);
    }

    this.notEmpty.signal();
    return prevTask;
  }

  @Override
  public void run() {
    while (this.running) {
      this.lock.lock();
      try {
        while (this.positions.isEmpty()) {
          this.notEmpty.await();
        }

        this.tasks.remove(this.positions.poll()).run();

        this.notFull.signal();
      } catch (InterruptedException e) {
        break;
      } finally {
        this.lock.unlock();
      }
    }
  }

  public void close() {
    this.lock.lock();
    try {
      this.running = false;
      this.thread.interrupt();

      while (!this.positions.isEmpty()) {
        Runnable task = this.tasks.remove(this.positions.poll());
        if (task instanceof WriteTask) {
          task.run();
        }
      }
    } finally {
      this.lock.unlock();
    }
  }
  
  private abstract class TimedTask implements Runnable {

    private final RollingTimer.Instance waitTimer = statistics.diskCacheWaitTime.start();
    private final RollingTimer runTimer;

    public TimedTask(RollingTimer runTimer) {
      this.runTimer = runTimer;
    }
    
    @Override
    public final void run() {
      waitTimer.stop();

      var timer = runTimer.start();
      try {
        execute();
      } finally {
        timer.stop();
      }
    }
    
    protected abstract void execute();
  }

  private class WriteTask extends TimedTask {

    private final ChunkCacheKey key;
    private final ChunkCacheEntry chunk;

    public WriteTask(ChunkCacheKey key, ChunkCacheEntry chunk) {
      super(statistics.diskCacheWriteTime);
      this.key = key;
      this.chunk = chunk;
    }

    @Override
    public void execute() {
      try {
        serializer.write(key, chunk);
      } catch (IOException e) {
        OfcLogger.error(e);
      }
    }
  }

  private class ReadTask extends TimedTask {

    private final ChunkCacheKey key;
    private final CompletableFuture<@Nullable ChunkCacheEntry> future;

    public ReadTask(ChunkCacheKey key, CompletableFuture<@Nullable ChunkCacheEntry> future) {
      super(statistics.diskCacheReadTime);
      this.key = key;
      this.future = future;
    }

    @Override
    public void execute() {
      try {
        future.complete(serializer.read(key));
      } catch (Exception e) {
        future.completeExceptionally(e);
      }
    }
  }
}
