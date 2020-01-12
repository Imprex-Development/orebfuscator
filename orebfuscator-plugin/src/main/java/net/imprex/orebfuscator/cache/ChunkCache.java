package net.imprex.orebfuscator.cache;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.config.CacheConfig;

import net.imprex.orebfuscator.util.ChunkPosition;

public class ChunkCache {

	private final CacheConfig cacheConfig;

	private final Cache<ChunkPosition, ChunkCacheEntry> cache;
	private final ChunkCacheSerializer serializer;

	public ChunkCache(Orebfuscator orebfuscator) {
		this.cacheConfig = orebfuscator.getConfigManager().getConfig().getCacheConfig();

		this.cache = CacheBuilder.newBuilder()
				.maximumSize(this.cacheConfig.maximumSize())
				.expireAfterAccess(this.cacheConfig.expireAfterAccess(), TimeUnit.SECONDS)
				.removalListener(this::onRemoval).build();

		this.serializer = new ChunkCacheSerializer(this.cacheConfig);
	}

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

	public ChunkCacheEntry get(ChunkPosition key, Function<ChunkPosition, ChunkCacheEntry> mappingFunction) {
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

	public void invalidate(ChunkPosition key) {
		this.cache.invalidate(key);
	}

	public void invalidateAll() {
		this.cache.invalidateAll();
		this.serializer.closeRegionFileCache(); // TODO move this to ChunkCache in nms
	}
}
