package net.imprex.orebfuscator.cache;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;

import net.imprex.cache.HybridCache;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ChunkCache {

	private static final HybridCache<ChunkPosition, ChunkCacheEntry> CACHE = new HybridCache<>(new ChunkCacheSerializer());

	public static ChunkCacheEntry get(ChunkPosition key, Function<ChunkPosition, ChunkCacheEntry> mappingFunction) {
		return CACHE.get(key, mappingFunction);
	}

	private final Cache<ChunkPosition, ChunkCacheEntry> cache = CacheBuilder.newBuilder()
			.maximumSize(65_536L)
			.expireAfterAccess(30, TimeUnit.SECONDS)
			.removalListener(this::onRemoval).build();

	private final ChunkCacheSerializer serializer = new ChunkCacheSerializer();

	private void onRemoval(RemovalNotification<ChunkPosition, ChunkCacheEntry> notification) {
		if (notification.wasEvicted()) {
			try {
				this.serializer.write(notification.getKey(), notification.getValue());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private ChunkCacheEntry load(ChunkPosition key) {	
		try {
			return this.serializer.read(key);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ChunkCacheEntry getd(ChunkPosition key, Function<ChunkPosition, ChunkCacheEntry> mappingFunction) {
		Objects.requireNonNull(mappingFunction);

		ChunkCacheEntry cacheEntry = this.cache.getIfPresent(key);
		if (cacheEntry == null) {
			cacheEntry = this.load(key);
			if (cacheEntry == null) {
				cacheEntry = mappingFunction.apply(key);
			}
			this.cache.put(key, Objects.requireNonNull(cacheEntry));
		}
		return cacheEntry;
	}

	public void clear() {
		this.cache.invalidateAll();
	}
}
