package net.imprex.orebfuscator.config;

import org.bukkit.configuration.ConfigurationSection;

public class OrebfuscatorConfig implements Config {

	private final OrebfuscatorCacheConfig cacheConfig = new OrebfuscatorCacheConfig();

	public void serialize(ConfigurationSection section) {
		ConfigurationSection cacheSection = section.getConfigurationSection("cache");
		if (cacheSection != null) {
			this.cacheConfig.serialize(cacheSection);
		}

	}

	@Override
	public CacheConfig cache() {
		return this.cacheConfig;
	}
}
