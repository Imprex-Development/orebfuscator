package com.lishid.orebfuscator.config;

import java.nio.file.Path;

public interface CacheConfig {

	boolean enabled();

	Path baseDirectory();

	int maximumSize();

	long expireAfterAccess();
}
