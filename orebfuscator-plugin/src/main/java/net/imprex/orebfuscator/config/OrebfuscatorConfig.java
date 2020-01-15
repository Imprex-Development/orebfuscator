package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import com.lishid.orebfuscator.Orebfuscator;

public class OrebfuscatorConfig implements Config {

	private final OrebfuscatorCacheConfig cacheConfig = new OrebfuscatorCacheConfig();
	private final List<WorldConfig> worlds = new ArrayList<>();

	public void serialize(ConfigurationSection section) {
		ConfigurationSection cacheSection = section.getConfigurationSection("cache");
		if (cacheSection != null) {
			this.cacheConfig.serialize(cacheSection);
		} else {
			Orebfuscator.LOGGER.warning("config section 'cache' is missing, using default one");
		}

		List<?> worldSection = section.getList("world", Collections.emptyList());
		if (worldSection != null) {
			for (Object world : worldSection) {
				System.out.println(world != null ? world.getClass() : "null");
			}
		} else {
			Orebfuscator.LOGGER.warning("config section 'world' is missing");
		}
	}

	@Override
	public CacheConfig cache() {
		return this.cacheConfig;
	}

	@Override
	public List<WorldConfig> worlds() {
		return this.worlds;
	}
}
