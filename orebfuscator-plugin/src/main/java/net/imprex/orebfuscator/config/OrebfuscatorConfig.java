package net.imprex.orebfuscator.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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

import net.imprex.orebfuscator.NmsInstance;

public class OrebfuscatorConfig implements Config {

	private static final int CONFIG_VERSION = 14;

	private final OrebfuscatorGeneralConfig generalConfig = new OrebfuscatorGeneralConfig();
	private final OrebfuscatorCacheConfig cacheConfig = new OrebfuscatorCacheConfig();
	private final List<OrebfuscatorWorldConfig> worlds = new ArrayList<>();

	private final Map<World, OrebfuscatorWorldConfig> worldToConfig = new HashMap<>();

	private final Plugin plugin;

	public OrebfuscatorConfig(Plugin plugin) {
		this.plugin = plugin;

		this.reload();
	}

	public void reload() {
		this.createConfigIfNotExist();
		this.plugin.reloadConfig();

		this.serialize(this.plugin.getConfig());

		// TODO close old nms instance
		NmsInstance.initialize(this);

		this.initialize();
	}

	private void createConfigIfNotExist() {
		Path path = this.plugin.getDataFolder().toPath().resolve("config.yml");

		if (Files.notExists(path)) {
			try {
				Matcher matcher = Globals.NMS_PATTERN.matcher(Globals.SERVER_VERSION);

				if (!matcher.find()) {
					throw new RuntimeException("WTF is this version!?");
				}

				String configVersion = matcher.group(1) + "." + matcher.group(2);

				if (Files.notExists(path.getParent())) {
					Files.createDirectories(path.getParent());
				}

				Files.copy(Orebfuscator.class.getResourceAsStream("/resources/config-" + configVersion + ".yml"), path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void serialize(ConfigurationSection section) {
		if (section.getInt("version", -1) != CONFIG_VERSION) {
			throw new RuntimeException("Current config is not up to date, please delete your config");
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

		List<?> worldSectionList = section.getList("world", Collections.emptyList());
		if (worldSectionList != null) {
			List<ConfigurationSection> sectionList = this.serializeSectionList(section, "world", worldSectionList);
			for (ConfigurationSection worldSection : sectionList) {
				OrebfuscatorWorldConfig worldConfig = new OrebfuscatorWorldConfig();
				worldConfig.serialize(worldSection);
				this.worlds.add(worldConfig);
			}
		} else {
			Orebfuscator.LOGGER.warning("config section 'world' is missing");
		}
	}

	private List<ConfigurationSection> serializeSectionList(ConfigurationSection parentSection, String path,
			List<?> sectionList) {
		List<ConfigurationSection> sections = new ArrayList<>();
		for (int i = 0; i < sectionList.size(); i++) {
			Object section = sectionList.get(i);
			if (section instanceof Map) {
				sections.add(ConfigParser.convertMapsToSections((Map<?, ?>) section,
						parentSection.createSection(path + "-" + i)));
			}
		}
		return sections;
	}

	private void initialize() {
		for (OrebfuscatorWorldConfig worldConfig : this.worlds) {
			worldConfig.initialize();

			for (World world : worldConfig.worlds()) {
				if (this.worldToConfig.containsKey(world)) {
					Orebfuscator.LOGGER
							.warning("world " + world.getName() + " has more than one config choosing first one");
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
	public OrebfuscatorWorldConfig world(World world) {
		return this.worldToConfig.get(Objects.requireNonNull(world));
	}
}
