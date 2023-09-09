package net.imprex.orebfuscator.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ObfuscationCache {

	private final Orebfuscator orebfuscator;
	private final CacheConfig cacheConfig;

	private final Cache<ChunkPosition, ObfuscationResult> cache;
	private final AsyncChunkSerializer serializer;

	public ObfuscationCache(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		this.cacheConfig = orebfuscator.getOrebfuscatorConfig().cache();

		this.cache = CacheBuilder.newBuilder().maximumSize(this.cacheConfig.maximumSize())
				.expireAfterAccess(this.cacheConfig.expireAfterAccess(), TimeUnit.MILLISECONDS)
				.removalListener(this::onRemoval).build();

		if (this.cacheConfig.enableDiskCache()) {
			this.serializer = new AsyncChunkSerializer(orebfuscator);
		} else {
			this.serializer = null;
		}

		if (this.cacheConfig.enabled() && this.cacheConfig.deleteRegionFilesAfterAccess() > 0) {
			Bukkit.getScheduler().runTaskTimerAsynchronously(orebfuscator, new CacheFileCleanupTask(orebfuscator), 0,
					3_600_000L);
		}
	}

	private void onRemoval(RemovalNotification<ChunkPosition, ObfuscationResult> notification) {
		// don't serialize invalidated chunks since this would require locking the main
		// thread and wouldn't bring a huge improvement
		if (this.cacheConfig.enableDiskCache() && notification.wasEvicted() && !this.orebfuscator.isGameThread()) {
			this.serializer.write(notification.getKey(), notification.getValue());
		}
	}

	public CompletableFuture<ObfuscationResult> get(ObfuscationRequest request) {
		ChunkPosition key = request.getPosition();

		ObfuscationResult cacheChunk = this.cache.getIfPresent(key);
		if (request.isValid(cacheChunk)) {
			return request.complete(cacheChunk);
		}

		if (this.cacheConfig.enableDiskCache()) {

			// compose async in order for the serializer to continue its work
			this.serializer.read(key).thenComposeAsync(diskChunk -> {
				if (request.isValid(diskChunk)) {
					return request.complete(diskChunk);
				} else {
					return request.submitForObfuscation();
				}
			}).whenComplete((chunk, throwable) -> {
				// if successful add chunk to in-memory cache
				if (chunk != null) {
					this.cache.put(key, chunk);
				}
			});
		} else {

			request.submitForObfuscation().thenAccept(chunk -> {
				// if successful add chunk to in-memory cache
				if (chunk != null) {
					this.cache.put(key, chunk);
				}
			});
		}

		return request.getFuture();
	}

	public void invalidate(ChunkPosition key) {
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
