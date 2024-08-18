package net.imprex.orebfuscator.config;

import java.nio.file.Path;

import net.imprex.orebfuscator.util.ChunkPosition;

public interface CacheConfig {

	boolean enabled();

	int maximumSize();

	long expireAfterAccess();
	
	boolean enableDiskCache();

	Path baseDirectory();

	Path regionFile(ChunkPosition chunkPosition);

	int maximumOpenRegionFiles();

	long deleteRegionFilesAfterAccess();

	int maximumTaskQueueSize();
}
