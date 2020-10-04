package net.imprex.orebfuscator.config;

import java.nio.file.Path;

import net.imprex.orebfuscator.util.ChunkPosition;

public interface CacheConfig {

	boolean enabled();
	void enabled(boolean enabled);

	Path baseDirectory();
	void baseDirectory(Path path);

	Path regionFile(ChunkPosition chunkPosition);

	int maximumOpenRegionFiles();
	/**
	 * @param count
	 * @throws IllegalArgumentException When the count value is lower than one
	 */
	void maximumOpenRegionFiles(int count);

	long deleteRegionFilesAfterAccess();
	/**
	 * @param expire
	 * @throws IllegalArgumentException When the expire value is lower than one
	 */
	void deleteRegionFilesAfterAccess(long expire);

	int maximumSize();
	/**
	 * @param size
	 * @throws IllegalArgumentException When the size value is lower than one
	 */
	void maximumSize(int size);

	long expireAfterAccess();
	/**
	 * @param expire
	 * @throws IllegalArgumentException When the expire value is lower than one
	 */
	void expireAfterAccess(long expire);

	int maximumTaskQueueSize();
	/**
	 * @param size
	 * @throws IllegalArgumentException When the expire value is lower than one
	 */
	void maximumTaskQueueSize(int size);

	int protocolLibThreads();
	void protocolLibThreads(int threads);
}
