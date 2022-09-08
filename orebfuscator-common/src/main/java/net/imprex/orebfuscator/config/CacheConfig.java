package net.imprex.orebfuscator.config;

import java.nio.file.Path;

import net.imprex.orebfuscator.util.ChunkPosition;

public interface CacheConfig {

	boolean enabled();

	Path baseDirectory();

	Path regionFile(ChunkPosition chunkPosition);

	int maximumOpenRegionFiles();

	long deleteRegionFilesAfterAccess();
	
	boolean enableDiskCache();

	int maximumSize();

	long expireAfterAccess();

	int maximumTaskQueueSize();
}
