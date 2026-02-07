package dev.imprex.orebfuscator.statistics;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import dev.imprex.orebfuscator.config.api.CacheConfig;
import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.util.RollingAverage;
import dev.imprex.orebfuscator.util.RollingTimer;

public class CacheStatistics implements StatisticsSource {

  private final CacheConfig config;

  private final AtomicLong cacheHitCountMemory = new AtomicLong(0);
  private final AtomicLong cacheHitCountDisk = new AtomicLong(0);
  private final AtomicLong cacheMissCount = new AtomicLong(0);

  private final AtomicLong memoryCacheByteSize = new AtomicLong(0);
  private LongSupplier memoryCacheEntryCount = () -> 0;

  private LongSupplier diskCacheQueueLength = () -> 0;
  private final RollingAverage diskCacheReadBytes = new RollingAverage(4096);
  private final RollingAverage diskCacheWriteBytes = new RollingAverage(4096);
  public final RollingTimer diskCacheWaitTime = new RollingTimer(4096);
  public final RollingTimer diskCacheReadTime = new RollingTimer(4096);
  public final RollingTimer diskCacheWriteTime = new RollingTimer(4096);

  public CacheStatistics(Config config) {
    this.config = config.cache();
  }

  public void onCacheHitMemory() {
    this.cacheHitCountMemory.incrementAndGet();
  }

  public void onCacheHitDisk() {
    this.cacheHitCountDisk.incrementAndGet();
  }

  public void onCacheMiss() {
    this.cacheMissCount.incrementAndGet();
  }

  public void onCacheSizeChange(int delta) {
    this.memoryCacheByteSize.addAndGet(delta);
  }

  public void setMemoryCacheEntryCount(LongSupplier supplier) {
    this.memoryCacheEntryCount = Objects.requireNonNull(supplier);
  }

  public void setDiskCacheQueueLength(LongSupplier supplier) {
    this.diskCacheQueueLength = Objects.requireNonNull(supplier);
  }

  public void onDiskCacheRead(long bytes) {
    this.diskCacheReadBytes.add(bytes);
  }

  public void onDiskCacheWrite(long bytes) {
    this.diskCacheWriteBytes.add(bytes);
  }

  public void add(StringJoiner joiner) {
    long cacheHitCountMemory = this.cacheHitCountMemory.get();
    long cacheHitCountDisk = this.cacheHitCountDisk.get();
    long cacheMissCount = this.cacheMissCount.get();
    long totalCount = cacheHitCountMemory + cacheHitCountDisk + cacheMissCount;

    double memoryHitRate = 0.0d;
    double diskHitRate = 0.0d;
    double missRate = 1.0d;
    if (totalCount > 0) {
      memoryHitRate = (double) cacheHitCountMemory / totalCount;
      diskHitRate = (double) cacheHitCountDisk / totalCount;
      missRate = 1d - (memoryHitRate + diskHitRate);
    }

    joiner.add(String.format(" - cacheHitRate (memory/disk/miss): %s / %s / %s",
        percent(memoryHitRate), percent(diskHitRate), percent(missRate)));

    long memoryCacheByteSize = this.memoryCacheByteSize.get();
    long memoryCacheEntryCount = this.memoryCacheEntryCount.getAsLong();

    long memoryCacheBytesPerEntry = 0;
    if (memoryCacheByteSize > 0) {
      memoryCacheBytesPerEntry = memoryCacheByteSize / memoryCacheEntryCount;
    }

    joiner.add(String.format(" - memoryCache (count/bytesPerEntry): %s / %s ",
        memoryCacheEntryCount, bytes(memoryCacheBytesPerEntry)));

    if (this.config.enableDiskCache()) {
      long diskCacheQueueLength = this.diskCacheQueueLength.getAsLong();

      joiner.add(String.format(" - diskCache (queue): %s", diskCacheQueueLength));

      long diskCacheWaitTime = (long) this.diskCacheWaitTime.average();
      long diskCacheReadTime = (long) this.diskCacheReadTime.average();
      long diskCacheWriteTime = (long) this.diskCacheWriteTime.average();

      joiner.add(String.format(" - diskCacheTime (wait/read/write): %s / %s / %s",
          time(diskCacheWaitTime), time(diskCacheReadTime), time(diskCacheWriteTime)));

      long diskCacheReadBytes = (long) this.diskCacheReadBytes.average();
      long diskCacheWriteBytes = (long) this.diskCacheWriteBytes.average();

      double diskCacheReadTimeSeconds = 1d;
      if (diskCacheReadTime > 0) {
        diskCacheReadTimeSeconds = (double) diskCacheReadTime / (double) TimeUnit.SECONDS.toNanos(1);
      }
      long diskCacheReadBytesPerSecond = Math.round((double) diskCacheReadBytes / diskCacheReadTimeSeconds);

      double diskCacheWriteTimeSeconds = 1d;
      if (diskCacheWriteTime > 0) {
        diskCacheWriteTimeSeconds = (double) diskCacheWriteTime / (double) TimeUnit.SECONDS.toNanos(1);
      }
      long diskCacheWriteBytesPerSecond = Math.round((double) diskCacheWriteBytes / diskCacheWriteTimeSeconds);

      joiner.add(String.format(" - diskCacheThroughput (read/write): %s/s / %s/s",
          bytes(diskCacheReadBytesPerSecond), bytes(diskCacheWriteBytesPerSecond)));

      joiner.add(String.format(" - diskCacheTaskSize (read/write): %s / %s",
          bytes(diskCacheReadBytes), bytes(diskCacheWriteBytes)));
    }
  }

  @Override
  public void debug(BiConsumer<String, String> consumer) {
    consumer.accept("cacheHitCountMemory", Long.toString(cacheHitCountMemory.get()));
    consumer.accept("cacheHitCountDisk", Long.toString(cacheHitCountDisk.get()));
    consumer.accept("cacheMissCount", Long.toString(cacheMissCount.get()));

    consumer.accept("memoryCacheByteSize", Long.toString(memoryCacheByteSize.get()));
    consumer.accept("memoryCacheEntryCount", Long.toString(memoryCacheEntryCount.getAsLong()));

    consumer.accept("diskCacheQueueLength", Long.toString(diskCacheQueueLength.getAsLong()));
    consumer.accept("diskCacheReadBytes", diskCacheReadBytes.debugLong(this::bytes));
    consumer.accept("diskCacheWriteBytes", diskCacheWriteBytes.debugLong(this::bytes));
    consumer.accept("diskCacheWaitTime", diskCacheWaitTime.debugLong(this::time));
    consumer.accept("diskCacheReadTime", diskCacheReadTime.debugLong(this::time));
    consumer.accept("diskCacheWriteTime", diskCacheWriteTime.debugLong(this::time));
  }
}
