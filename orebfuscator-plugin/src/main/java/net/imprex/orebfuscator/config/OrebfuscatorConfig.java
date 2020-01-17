package net.imprex.orebfuscator.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.utils.Globals;

public class OrebfuscatorConfig implements Config {

	private static final int CONFIG_VERSION = 14;

	private final OrebfuscatorGeneralConfig generalConfig = new OrebfuscatorGeneralConfig();
	private final OrebfuscatorCacheConfig cacheConfig = new OrebfuscatorCacheConfig();
	private final List<WorldConfig> worlds = new ArrayList<>();

	private final Map<World, WorldConfig> worldToConfig = new HashMap<>();

	private final Plugin plugin;

	public OrebfuscatorConfig(Plugin plugin) {
		this.plugin = plugin;

		this.reload();
	}

	public void reload() {
		this.createConfigIfNotExist();
		this.plugin.reloadConfig();

		this.serialize(this.plugin.getConfig());

		this.initialize();
	}

	private void createConfigIfNotExist() {
		Path dataFolder = this.plugin.getDataFolder().toPath();
		Path path = dataFolder.resolve("config.yml");

		if (Files.notExists(path)) {
			try {
				Matcher matcher = Globals.NMS_PATTERN.matcher(Globals.SERVER_VERSION);

				if (!matcher.find()) {
					throw new RuntimeException("Can't parse server version " + Globals.SERVER_VERSION);
				}

				String configVersion = matcher.group(1) + "." + matcher.group(2);

				if (Files.notExists(dataFolder)) {
					Files.createDirectories(dataFolder);
				}

				Files.copy(Orebfuscator.class.getResourceAsStream("/resources/config-" + configVersion + ".yml"), path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void serialize(ConfigurationSection section) {
		if (section.getInt("version", -1) != CONFIG_VERSION) {
			throw new RuntimeException("config is not up to date, please delete your config");
		}

		ConfigurationSection generalSection = section.getConfigurationSection("general");
		if (generalSection != null) {
			this.generalConfig.serialize(generalSection);
		} else {
			Orebfuscator.LOGGER.warning("config section 'general' is missing, using default one");
		}

		ConfigurationSection cacheSection = section.getConfigurationSection("cache");
		if (cacheSection != null) {
			this.cacheConfig.serialize(cacheSection);
		} else {
			Orebfuscator.LOGGER.warning("config section 'cache' is missing, using default one");
		}

		List<ConfigurationSection> worldSectionList = ConfigParser.serializeSectionList(section, "world");
		if (!worldSectionList.isEmpty()) {
			for (ConfigurationSection worldSection : worldSectionList) {
				OrebfuscatorWorldConfig worldConfig = new OrebfuscatorWorldConfig();
				worldConfig.serialize(worldSection);
				this.worlds.add(worldConfig);
			}
		} else {
			Orebfuscator.LOGGER.warning("config section 'world' is missing or empty");
		}
	}

	private void initialize() {
		for (WorldConfig worldConfig : this.worlds) {
			for (World world : worldConfig.worlds()) {
				if (this.worldToConfig.containsKey(world)) {
					Orebfuscator.LOGGER
							.warning("world " + world.getName() + " has more than one world config choosing first one");
				} else {
					this.worldToConfig.put(world, worldConfig);
				}
			}
		}

		for (World world : Bukkit.getWorlds()) {
			if (!this.worldToConfig.containsKey(world)) {
				throw new IllegalStateException("world " + world.getName() + " is missing a world config");
			}
		}
	}

	@Override
	public GeneralConfig general() {
		return this.generalConfig;
	}

	@Override
	public CacheConfig cache() {
		return this.cacheConfig;
	}

	@Override
	public List<WorldConfig> worlds() {
		return this.worlds;
	}

	@Override
	public WorldConfig world(World world) {
		return this.worldToConfig.get(Objects.requireNonNull(world));
	}
}
