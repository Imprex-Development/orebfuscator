package net.imprex.orebfuscator.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.command.CommandSender;
import org.openjdk.jol.info.GraphLayout;

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
				.expireAfterAccess(this.cacheConfig.expireAfterAccess(), TimeUnit.MILLISECONDS)
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
		OrebfuscatorCompatibility.runAsyncNow(() -> {
			long cacheSize = this.cache.size();

			double bytes = this.cache.asMap().values().stream()
					.mapToLong(entry -> GraphLayout.parseInstance(entry).totalSize())
					.sum() / 1024d;
			double decompressedBytes = this.cache.asMap().values().stream()
					.map(entry -> entry.toResult())
					.mapToLong(entry -> GraphLayout.parseInstance(entry).totalSize())
					.sum() / 1024d;

			sender.sendMessage(String.format("count: %d, heap: %.1f/%.1f MiB, avg: %.1f/%.1f KiB, pct: %.1f%%", 
					cacheSize,
					(bytes / 1024d), (decompressedBytes / 1024d),
					(bytes / cacheSize), (decompressedBytes / cacheSize),
					(bytes / decompressedBytes * 100d)));
		});
	}

	private void onRemoval(RemovalNotification<ChunkPosition, CompressedObfuscationResult> notification) {
		// don't serialize invalidated chunks since this would require locking the main
		// thread and wouldn't bring a huge improvement
		if (this.cacheConfig.enableDiskCache() && notification.wasEvicted() && !this.orebfuscator.isGameThread()) {
			this.serializer.write(notification.getKey(), notification.getValue());
		}
	}

	private void requestObfuscation(ObfuscationRequest request) {
		request.submitForObfuscation().thenAccept(chunk -> {
			var compressedChunk = CompressedObfuscationResult.create(chunk);
			if (compressedChunk != null) {
				this.cache.put(request.getPosition(), compressedChunk);
			}
		});
	}

	public CompletableFuture<ObfuscationResult> get(ObfuscationRequest request) {
		ChunkPosition key = request.getPosition();

		CompressedObfuscationResult cacheChunk = this.cache.getIfPresent(key);
		if (cacheChunk != null && cacheChunk.isValid(request)) {
			cacheChunk.toResult().ifPresent(request::complete);
		}
		// only access disk cache if we couldn't find the chunk in memory cache
		else if (cacheChunk == null && this.cacheConfig.enableDiskCache()) {
			this.serializer.read(key).whenComplete((diskChunk, throwable) -> {
				if (diskChunk != null && diskChunk.isValid(request)) {
					// add valid disk cache entry to in-memory cache
					this.cache.put(key, diskChunk);
					diskChunk.toResult().ifPresentOrElse(request::complete,
							// request obfuscation if decoding failed
							() -> this.requestObfuscation(request));
				} else {
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
			this.requestObfuscation(request);
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
