package net.imprex.orebfuscator.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;

import dev.imprex.orebfuscator.config.api.CacheConfig;
import dev.imprex.orebfuscator.util.ChunkCacheKey;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.OrebfuscatorStatistics;
import net.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;

public class ObfuscationCache {

	private final CacheConfig cacheConfig;
	private final OrebfuscatorStatistics statistics;

	private final Cache<ChunkCacheKey, CacheChunkEntry> cache;
	private final AsyncChunkSerializer serializer;

	public ObfuscationCache(Orebfuscator orebfuscator) {
		this.cacheConfig = orebfuscator.getOrebfuscatorConfig().cache();
		this.statistics = orebfuscator.getStatistics();

		this.cache = CacheBuilder.newBuilder()
				.maximumSize(this.cacheConfig.maximumSize())
				.expireAfterAccess(this.cacheConfig.expireAfterAccess(), TimeUnit.MILLISECONDS)
				.removalListener(this::onRemoval)
				.build();
		this.statistics.setMemoryCacheSizeSupplier(() -> this.cache.size());

		if (this.cacheConfig.enableDiskCache()) {
			this.serializer = new AsyncChunkSerializer(orebfuscator);
		} else {
			this.serializer = null;
		}

		if (this.cacheConfig.enabled() && this.cacheConfig.deleteRegionFilesAfterAccess() > 0) {
			OrebfuscatorCompatibility.runAsyncAtFixedRate(new CacheFileCleanupTask(orebfuscator), 0, 72000L);
		}
	}

	private void onRemoval(@NotNull RemovalNotification<ChunkCacheKey, CacheChunkEntry> notification) {
		this.statistics.onCacheSizeChange(-notification.getValue().estimatedSize());

		// don't serialize invalidated chunks since this would require locking the main
		// thread and wouldn't bring a huge improvement
		if (this.cacheConfig.enableDiskCache() && notification.wasEvicted() && !OrebfuscatorCompatibility.isGameThread()) {
			this.serializer.write(notification.getKey(), notification.getValue());
		}
	}

	private void requestObfuscation(@NotNull ObfuscationRequest request) {
		request.submitForObfuscation().thenAccept(chunk -> {
			var compressedChunk = CacheChunkEntry.create(chunk);
			if (compressedChunk != null) {
				this.cache.put(request.getCacheKey(), compressedChunk);
				this.statistics.onCacheSizeChange(compressedChunk.estimatedSize());
			}
		});
	}

	@NotNull
	public CompletableFuture<ObfuscationResult> get(@NotNull ObfuscationRequest request) {
		ChunkCacheKey key = request.getCacheKey();

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
			this.serializer.read(key).whenComplete((diskChunk, throwable) -> {
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

				// request future doesn't care about serialzer failure because
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

		return request.getFuture();
	}

	public void invalidate(ChunkCacheKey key) {
		this.cache.invalidate(key);
	}

	public void close() {
		if (this.cacheConfig.enableDiskCache()) {
			// flush memory cache to disk on shutdown
			this.cache.asMap().entrySet().removeIf(entry -> {
				this.serializer.write(entry.getKey(), entry.getValue());
				return true;
			});

			this.serializer.close();
		}
	}
}
