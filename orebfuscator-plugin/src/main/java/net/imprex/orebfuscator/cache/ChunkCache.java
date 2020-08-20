package net.imprex.orebfuscator.cache;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Bukkit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.obfuscation.ObfuscatedChunk;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ChunkCache {

	private static final HashFunction HASH_FUNCTION = Hashing.murmur3_128();

	public static final byte[] hash(byte[] configHash, byte[] chunkData) {
		Hasher hasher = HASH_FUNCTION.newHasher();
		hasher.putBytes(configHash);
		hasher.putBytes(chunkData);
		return hasher.hash().asBytes();
	}

	private final CacheConfig cacheConfig;

	private final Cache<ChunkPosition, ObfuscatedChunk> cache;
	private final ChunkCacheSerializer serializer;

	public ChunkCache(Orebfuscator orebfuscator) {
		this.cacheConfig = orebfuscator.getOrebfuscatorConfig().cache();

		this.cache = CacheBuilder.newBuilder().maximumSize(this.cacheConfig.maximumSize())
				.expireAfterAccess(this.cacheConfig.expireAfterAccess(), TimeUnit.MILLISECONDS)
				.removalListener(this::onRemoval).build();

		this.serializer = new ChunkCacheSerializer();

		if (this.cacheConfig.enabled() && this.cacheConfig.deleteRegionFilesAfterAccess() > 0) {
			Bukkit.getScheduler().runTaskTimerAsynchronously(orebfuscator, new CacheCleanTask(orebfuscator), 0,
					3_600_000L);
		}
	}

	private void onRemoval(RemovalNotification<ChunkPosition, ObfuscatedChunk> notification) {
		if (notification.wasEvicted()) {
			try {
				this.serializer.write(notification.getKey(), notification.getValue());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private ObfuscatedChunk load(ChunkPosition key) {
		try {
			return this.serializer.read(key);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void get(ChunkCacheRequest request, Consumer<ObfuscatedChunk> consumer) {
		ChunkPosition key = request.getKey();
		byte[] hash = request.getHash();

		ObfuscatedChunk cacheEntry = this.cache.getIfPresent(key);
		if (cacheEntry != null && Arrays.equals(cacheEntry.getHash(), hash)) {
			consumer.accept(cacheEntry);
			return;
		}

		// check if disk cache entry is present and valid
		cacheEntry = this.load(key);
		if (cacheEntry != null && Arrays.equals(cacheEntry.getHash(), hash)) {
			this.cache.put(key, Objects.requireNonNull(cacheEntry));
			consumer.accept(cacheEntry);
			return;
		}

		// create new entry no valid ones found
		request.obfuscate(chunk -> {
			this.cache.put(key, Objects.requireNonNull(chunk));
			consumer.accept(chunk);
		});
	}

	public void invalidate(ChunkPosition key) {
		this.cache.invalidate(key);
		try {
			this.serializer.write(key, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void invalidateAll(boolean save) {
		if (save) {
			this.cache.asMap().entrySet().removeIf(entry -> {
				try {
					this.serializer.write(entry.getKey(), entry.getValue());
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;
			});
		} else {
			this.cache.invalidateAll();
		}
	}
}
