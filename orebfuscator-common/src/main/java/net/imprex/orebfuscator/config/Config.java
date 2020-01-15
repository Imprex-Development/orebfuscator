package net.imprex.orebfuscator.config;

import java.util.List;

public interface Config {

	CacheConfig cache();

	List<WorldConfig> worlds();
}
