package dev.imprex.orebfuscator.cache;

import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import dev.imprex.orebfuscator.config.api.CacheConfig;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.obfuscation.CacheRequest;
import dev.imprex.orebfuscator.obfuscation.ObfuscationPipeline;
import dev.imprex.orebfuscator.statistics.CacheStatistics;
import dev.imprex.orebfuscator.util.ChunkCacheKey;

public class ObfuscationCache {

  private final OrebfuscatorCore orebfuscator;
  private final CacheConfig cacheConfig;
  private final CacheStatistics statistics;
  private final ObfuscationPipeline pipeline;

  private final AbstractRegionFileCache<?> regionFileCache;
  private final Cache<ChunkCacheKey, CacheChunkEntry> cache;
  private final AsyncChunkSerializer serializer;

  public ObfuscationCache(OrebfuscatorCore orebfuscator) {
    this.orebfuscator = orebfuscator;
    this.cacheConfig = orebfuscator.config().cache();
    this.statistics = orebfuscator.statistics().cache;
    this.pipeline = orebfuscator.obfuscationPipeline();

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
      // TODO: OrebfuscatorCompatibility.runAsyncAtFixedRate(new CacheFileCleanupTask(orebfuscator,
      // regionFileCache), 0, 72000L);
    }
  }

  private void onRemoval(@NotNull RemovalNotification<ChunkCacheKey, CacheChunkEntry> notification) {
    this.statistics.onCacheSizeChange(-notification.getValue().estimatedSize());

    // don't serialize invalidated chunks since this would require locking the main
    // thread and wouldn't bring a huge improvement
    if (this.cacheConfig.enableDiskCache() && notification.wasEvicted() && !orebfuscator.isGameThread()) {
      this.serializer.write(notification.getKey(), notification.getValue());
    }
  }

  private void requestObfuscation(@NotNull CacheRequest request) {
    this.pipeline.submitRequest(request).thenAccept(response -> {
      var compressedChunk = CacheChunkEntry.create(request, response);
      if (compressedChunk != null) {
        this.cache.put(request.cacheKey(), compressedChunk);
        this.statistics.onCacheSizeChange(compressedChunk.estimatedSize());
      }
    });
  }

  @NotNull
  public void get(@NotNull CacheRequest request) {
    ChunkCacheKey key = request.cacheKey();

    CacheChunkEntry cacheChunk = this.cache.getIfPresent(key);
    if (cacheChunk != null && cacheChunk.isValid(request)) {
      this.statistics.onCacheHitMemory();

      // complete request
      cacheChunk.toResult().ifPresentOrElse(request::complete,
          // request obfuscation if decoding failed
          () -> this.requestObfuscation(request));
    }
    // only access disk cache if we couldn't find the chunk in memory cache
    else if (cacheChunk == null && this.cacheConfig.enableDiskCache()) {
      this.serializer.read(key).whenCompleteAsync((diskChunk, throwable) -> {
        if (diskChunk != null && diskChunk.isValid(request)) {
          this.statistics.onCacheHitDisk();

          // add valid disk cache entry to in-memory cache
          this.cache.put(key, diskChunk);
          this.statistics.onCacheSizeChange(diskChunk.estimatedSize());

          // complete request
          diskChunk.toResult().ifPresentOrElse(request::complete,
              // request obfuscation if decoding failed
              () -> this.requestObfuscation(request));
        } else {
          this.statistics.onCacheMiss();

          // request obfuscation if disk cache missing
          this.requestObfuscation(request);
        }

        // request future doesn't care about serializer failure because
        // we simply request obfuscation on failure
        if (throwable != null) {
          throwable.printStackTrace();
        }
      });
    }
    // request obfuscation if cache missed
    else {
      this.statistics.onCacheMiss();
      this.requestObfuscation(request);
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
