package net.imprex.orebfuscator.config;

import java.nio.file.Path;

public interface CacheConfig {

	boolean enabled();

	// TODO don't forget to check if path is subdir of bukkit world path
	Path baseDirectory();

	int maximumOpenRegionFiles();

	long deleteRegionFilesAfterAccess();

	int maximumSize();

	long expireAfterAccess();
}
