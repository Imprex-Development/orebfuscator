package net.imprex.orebfuscator.cache;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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

	private final CacheConfig cacheConfig;

	private final Cache<ChunkPosition, ObfuscationResult> cache;
	private final AsyncChunkSerializer serializer;

	public ObfuscationCache(Orebfuscator orebfuscator) {
		this.cacheConfig = orebfuscator.getOrebfuscatorConfig().cache();

		this.cache = CacheBuilder.newBuilder().maximumSize(this.cacheConfig.maximumSize())
				.expireAfterAccess(this.cacheConfig.expireAfterAccess(), TimeUnit.MILLISECONDS)
				.removalListener(this::onRemoval).build();

		this.serializer = new AsyncChunkSerializer(orebfuscator);

		if (this.cacheConfig.enabled() && this.cacheConfig.deleteRegionFilesAfterAccess() > 0) {
			Bukkit.getScheduler().runTaskTimerAsynchronously(orebfuscator, new CacheCleanTask(orebfuscator), 0,
					3_600_000L);
		}
	}

	private void onRemoval(RemovalNotification<ChunkPosition, ObfuscationResult> notification) {
		if (notification.wasEvicted()) {
			this.serializer.write(notification.getKey(), notification.getValue());
		}
	}

	public CompletionStage<ObfuscationResult> get(ObfuscationRequest request) {
		ChunkPosition key = request.getPosition();

		ObfuscationResult cacheChunk = this.cache.getIfPresent(key);
		if (request.isValid(cacheChunk)) {
			return request.complete(cacheChunk);
		}

		this.serializer.read(key).thenCompose(diskChunk -> {
			if (request.isValid(diskChunk)) {
				return request.complete(diskChunk);
			} else {
				return request.submitForObfuscation().exceptionally(throwable -> null);
			}
		}).thenAccept(chunk -> {
			if (chunk != null) {
				this.cache.put(key, chunk);
			}
		});

		return request.getFuture();
	}

	public void invalidate(ChunkPosition key) {
		this.cache.invalidate(key);
		this.serializer.invalidate(key);
	}

	public void close() {
		this.cache.asMap().entrySet().removeIf(entry -> {
			this.serializer.write(entry.getKey(), entry.getValue());
			return true;
		});

		this.serializer.close();
	}
}
