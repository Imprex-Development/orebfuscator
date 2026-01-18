package dev.imprex.orebfuscator.cache;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import dev.imprex.orebfuscator.config.api.CacheConfig;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.obfuscation.ObfuscationResponse;
import dev.imprex.orebfuscator.statistics.CacheStatistics;
import dev.imprex.orebfuscator.util.ChunkCacheKey;
import dev.imprex.orebfuscator.util.concurrent.OrebfuscatorExecutor;

@NullMarked
public class ObfuscationCache {

  private final OrebfuscatorCore orebfuscator;
  private final CacheConfig cacheConfig;
  private final CacheStatistics statistics;
  private final OrebfuscatorExecutor executor;

  private final AbstractRegionFileCache<?> regionFileCache;
  private final Cache<ChunkCacheKey, ChunkCacheEntry> cache;
  private final @Nullable AsyncChunkSerializer serializer;

  public ObfuscationCache(OrebfuscatorCore orebfuscator) {
    this.orebfuscator = orebfuscator;
    this.cacheConfig = orebfuscator.config().cache();
    this.statistics = orebfuscator.statistics().cache;
    this.executor = orebfuscator.executor();

    this.cache = CacheBuilder.newBuilder()
        .maximumSize(this.cacheConfig.maximumSize())
        .expireAfterAccess(this.cacheConfig.expireAfterAccess(), TimeUnit.MILLISECONDS)
        .removalListener(this::onRemoval)
        .build();
    this.statistics.setMemoryCacheEntryCount(this.cache::size);

    this.regionFileCache = orebfuscator.createRegionFileCache();

    if (this.cacheConfig.enableDiskCache()) {
      this.serializer = new AsyncChunkSerializer(orebfuscator, regionFileCache);
    } else {
      this.serializer = null;
    }

    if (this.cacheConfig.enabled() && this.cacheConfig.deleteRegionFilesAfterAccess() > 0) {
      var task = new CacheFileCleanupTask(orebfuscator.config(), regionFileCache);
      this.executor.scheduleAtFixedRate(task, 0, 1, TimeUnit.HOURS);
    }
  }

  private void onRemoval(RemovalNotification<ChunkCacheKey, ChunkCacheEntry> notification) {
    assert notification.getValue() != null;
    this.statistics.onCacheSizeChange(-notification.getValue().estimatedSize());

    // don't serialize invalidated chunks since this would require locking the main
    // thread and wouldn't bring a huge improvement
    if (this.serializer != null && notification.wasEvicted() && !orebfuscator.isGameThread()) {
      assert notification.getKey() != null;
      this.serializer.write(notification.getKey(), notification.getValue());
    }
  }

  public CompletionStage<Optional<ObfuscationResponse>> get(CacheRequest request) {
    return probeCaches(request).thenApplyAsync(response -> {
      if (response instanceof CacheResponse.Success success) {
        return Optional.of(success.entry().toResult());
      } else {
        this.statistics.onCacheMiss();
        return Optional.<ObfuscationResponse>empty();
      }
    }, this.executor).exceptionallyAsync(throwable -> {
      OfcLogger.error("An error occurred while trying to get cache entry for request: %s".formatted(request),
          throwable);
      return Optional.<ObfuscationResponse>empty();
    }, this.executor);
  }

  private CompletionStage<CacheResponse> probeCaches(CacheRequest request) {
    var future = CompletableFuture.supplyAsync(() -> probeMemory(request), this.executor);

    if (this.serializer != null) {
      future = future.thenComposeAsync(response ->
          // only access disk cache if we couldn't find an entry in memory cache
          response == CacheResponse.Failure.NOT_FOUND
              ? this.probeDisk(request)
              : CompletableFuture.completedStage(response)
          , this.executor);
    }

    return future;
  }

  private CacheResponse probeMemory(CacheRequest request) {
    ChunkCacheEntry cacheEntry = this.cache.getIfPresent(request.cacheKey());

    if (cacheEntry == null) {
      return CacheResponse.Failure.NOT_FOUND;
    } else if (!cacheEntry.isValid(request)) {
      // invalidate invalid in-memory cache entries
      this.cache.invalidate(request.cacheKey());
      return CacheResponse.Failure.MEMORY_INVALID;
    }

    this.statistics.onCacheHitMemory();
    return CacheResponse.success(cacheEntry);
  }

  private CompletionStage<CacheResponse> probeDisk(CacheRequest request) {
    return this.serializer.read(request.cacheKey()).thenApplyAsync(cacheEntry -> {
      if (cacheEntry == null) {
        return CacheResponse.Failure.NOT_FOUND;
      } else if (!cacheEntry.isValid(request)) {
        return CacheResponse.Failure.DISK_INVALID;
      }

      // add valid disk cache entry to in-memory cache
      this.cache.put(request.cacheKey(), cacheEntry);
      this.statistics.onCacheSizeChange(cacheEntry.estimatedSize());

      this.statistics.onCacheHitDisk();
      return CacheResponse.success(cacheEntry);
    }, this.executor);
  }

  public void add(CacheRequest request, ObfuscationResponse response) {
    try {
      var entry = ChunkCacheEntry.create(request, response);
      this.cache.put(request.cacheKey(), entry);
      this.statistics.onCacheSizeChange(entry.estimatedSize());
    } catch (Exception e) {
      OfcLogger.error("An error occurred while trying to cache entry for request: %s".formatted(request), e);
    }
  }

  public void invalidate(ChunkCacheKey key) {
    this.cache.invalidate(key);
  }

  public void close() {
    if (this.serializer != null) {
      // flush memory cache to disk on shutdown
      this.cache.asMap().entrySet().removeIf(entry -> {
        this.serializer.write(entry.getKey(), entry.getValue());
        return true;
      });

      this.serializer.close();
    }

    this.regionFileCache.clear();
  }
}
