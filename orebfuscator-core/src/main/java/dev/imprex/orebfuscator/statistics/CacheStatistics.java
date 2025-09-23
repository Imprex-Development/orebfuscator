package dev.imprex.orebfuscator.statistics;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CacheStatistics implements StatisticsSource {

  private final AtomicLong cacheHitCountMemory = new AtomicLong(0);
  private final AtomicLong cacheHitCountDisk = new AtomicLong(0);
  private final AtomicLong cacheMissCount = new AtomicLong(0);

  private final AtomicLong memoryCacheByteSize = new AtomicLong(0);
  private LongSupplier memoryCacheEntryCount = () -> 0;

  private LongSupplier diskCacheQueueLength = () -> 0;

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

  public void add(StringJoiner joiner) {
    long cacheHitCountMemory = this.cacheHitCountMemory.get();
    long cacheHitCountDisk = this.cacheHitCountDisk.get();
    long cacheMissCount = this.cacheMissCount.get();
    long totalCount = cacheHitCountMemory + cacheHitCountDisk + cacheMissCount;

    double memoryHitRate = 0.0d;
    double diskHitRate = 0.0d;
    double missRate = 100.0d;
    if (totalCount > 0) {
        memoryHitRate = (double) cacheHitCountMemory / totalCount;
        diskHitRate = (double) cacheHitCountDisk / totalCount;
        missRate = 100d - (memoryHitRate + diskHitRate);
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

    long diskCacheQueueLength = this.diskCacheQueueLength.getAsLong();

    joiner.add(String.format(" - diskCache (queue): %s ", diskCacheQueueLength));
  }

  public JsonElement json() {
    JsonObject object = new JsonObject();
    object.addProperty("cacheHitCountMemory", cacheHitCountMemory.get());
    object.addProperty("cacheHitCountDisk", cacheHitCountDisk.get());
    object.addProperty("cacheMissCount", cacheMissCount.get());
    object.addProperty("memoryCacheByteSize", memoryCacheByteSize.get());
    object.addProperty("memoryCacheEntryCount", memoryCacheEntryCount.getAsLong());
    object.addProperty("diskCacheQueueLength", diskCacheQueueLength.getAsLong());
    return object;
  }
}
