package net.imprex.orebfuscator.config;

import java.nio.file.Path;

public interface CacheConfig {

	boolean enabled();

	Path baseDirectory();

	int maximumOpenRegionFiles();

	int maximumSize();

	long expireAfterAccess();
}
