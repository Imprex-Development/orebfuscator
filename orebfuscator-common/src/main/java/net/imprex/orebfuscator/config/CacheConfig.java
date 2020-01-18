package net.imprex.orebfuscator.config;

import java.nio.file.Path;

public interface CacheConfig {

	boolean enabled();

	Path baseDirectory();

	int maximumOpenRegionFiles();

	long deleteRegionFilesAfterAccess();

	int maximumSize();

	long expireAfterAccess();
}
