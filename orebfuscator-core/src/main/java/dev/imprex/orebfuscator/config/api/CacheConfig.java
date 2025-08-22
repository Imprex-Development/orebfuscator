package dev.imprex.orebfuscator.config.api;

import java.nio.file.Path;

import dev.imprex.orebfuscator.util.ChunkCacheKey;

public interface CacheConfig {

	boolean enabled();

	int maximumSize();

	long expireAfterAccess();
	
	boolean enableDiskCache();

	Path baseDirectory();

	Path regionFile(ChunkCacheKey chunkPosition);

	int maximumOpenRegionFiles();

	long deleteRegionFilesAfterAccess();

	int maximumTaskQueueSize();
}
