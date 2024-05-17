package net.imprex.orebfuscator.cache;

import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ObfuscationCache {

	private final Orebfuscator orebfuscator;
	private final CacheConfig cacheConfig;

	private final Cache<ChunkPosition, CompressedObfuscationResult> cache;
	private final AsyncChunkSerializer serializer;

	public ObfuscationCache(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		this.cacheConfig = orebfuscator.getOrebfuscatorConfig().cache();

		this.cache = CacheBuilder.newBuilder()
				.maximumSize(this.cacheConfig.maximumSize())
//				.expireAfterAccess(this.cacheConfig.expireAfterAccess(), TimeUnit.MILLISECONDS)
				.removalListener(this::onRemoval)
				.build();

		if (this.cacheConfig.enableDiskCache()) {
			this.serializer = new AsyncChunkSerializer(orebfuscator);
		} else {
			this.serializer = null;
		}

		if (this.cacheConfig.enabled() && this.cacheConfig.deleteRegionFilesAfterAccess() > 0) {
			OrebfuscatorCompatibility.runAsyncAtFixedRate(new CacheFileCleanupTask(orebfuscator), 0, 72000L);
		}
	}

	public void printEstimatedSize(CommandSender sender) {
		long cacheSize = this.cache.size();
		long bytes = this.cache.asMap().values().stream()
				.mapToLong(entry -> 48 /* plain object size + fields */ + entry.getCompressedData().length)
				.sum();
		sender.sendMessage("size: " + cacheSize + ", bytes: " + (bytes / 1024 / 1024) + "MiB, avg: " + (bytes / cacheSize));

		long decompressedBytes = this.cache.asMap().values().stream()
				.map(entry -> entry.toResult())
				.mapToLong(entry -> 48 /* plain object size + fields */ + entry.getHash().length + entry.getData().length
						+ 24 + entry.getBlockEntities().size() * 72
						+ 24 + entry.getProximityBlocks().size() * 48)
				.sum();
		sender.sendMessage("size: " + cacheSize + ", bytes: " + (decompressedBytes / 1024 / 1024) + "MiB, avg: " + (decompressedBytes / cacheSize));
	}

	public long estimatedBytes() {
		return this.cache.asMap().values().stream()
				.mapToLong(entry -> 48 /* plain object size + fields */ + entry.getCompressedData().length)
				.sum();
	}

	private void onRemoval(RemovalNotification<ChunkPosition, CompressedObfuscationResult> notification) {
		// don't serialize invalidated chunks since this would require locking the main
		// thread and wouldn't bring a huge improvement
		if (this.cacheConfig.enableDiskCache() && notification.wasEvicted() && !this.orebfuscator.isGameThread()) {
			this.serializer.write(notification.getKey(), notification.getValue());
		}
	}

	public CompletableFuture<ObfuscationResult> get(ObfuscationRequest request) {
		ChunkPosition key = request.getPosition();

		CompressedObfuscationResult cacheChunk = this.cache.getIfPresent(key);
		if (cacheChunk != null && cacheChunk.isValid(request)) {
			return request.complete(cacheChunk.toResult());
		}

		if (this.cacheConfig.enableDiskCache()) {

			// compose async in order for the serializer to continue its work
			this.serializer.read(key)
				.thenComposeAsync(diskChunk -> {
					if (diskChunk != null && diskChunk.isValid(request)) {
						return request.complete(diskChunk.toResult());
					} else {
						return request.submitForObfuscation();
					}
				})
				.whenComplete((chunk, throwable) -> {
					try {
						// if successful add chunk to in-memory cache
						if (chunk != null) {
							this.cache.put(key, CompressedObfuscationResult.create(chunk));
						}
						if (throwable != null) {
							request.completeExceptionally(throwable);
						}
					} catch (Exception e) {
						request.completeExceptionally(e);
					}
				});
		} else {

			request.submitForObfuscation()
				.whenComplete((chunk, throwable) -> {
					try {
						// if successful add chunk to in-memory cache
						if (chunk != null) {
							this.cache.put(key, CompressedObfuscationResult.create(chunk));
						}
						if (throwable != null) {
							request.completeExceptionally(throwable);
						}
					} catch (Exception e) {
						request.completeExceptionally(e);
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
