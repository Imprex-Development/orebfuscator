package net.imprex.orebfuscator.cache;

import java.util.function.Function;

import net.imprex.cache.HybridCache;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ChunkCache {

	private static final HybridCache<ChunkPosition, ChunkCacheEntry> CACHE = new HybridCache<>(new ChunkCacheSerializer());

	public static ChunkCacheEntry get(ChunkPosition key, Function<ChunkPosition, ChunkCacheEntry> mappingFunction) {
		return CACHE.get(key, mappingFunction);
	}
}
