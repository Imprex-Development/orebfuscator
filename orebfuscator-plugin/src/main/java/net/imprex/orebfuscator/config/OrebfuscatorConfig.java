package net.imprex.orebfuscator.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.Plugin;

import com.google.common.hash.Hashing;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorConfig implements Config {

	private static final int CONFIG_VERSION = 1;

	private final OrebfuscatorGeneralConfig generalConfig = new OrebfuscatorGeneralConfig();
	private final OrebfuscatorCacheConfig cacheConfig = new OrebfuscatorCacheConfig();

	private final List<OrebfuscatorObfuscationConfig> obfuscationConfigs = new ArrayList<>();
	private final List<OrebfuscatorProximityConfig> proximityConfigs = new ArrayList<>();

	private final Map<World, OrebfuscatorConfig.WorldConfigs> worldConfigs = new WeakHashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private final Plugin plugin;

	private byte[] systemHash;

	public OrebfuscatorConfig(Plugin plugin) {
		this.plugin = plugin;

		this.load();
	}

	public void load() {
		this.createConfigIfNotExist();
		this.plugin.reloadConfig();
		this.deserialize(this.plugin.getConfig());
	}

	public void store() {
		ConfigurationSection section = this.plugin.getConfig();
		for (String path : section.getKeys(false)) {
			section.set(path, null);
		}

		this.serialize(section);
		this.plugin.saveConfig();
	}

	private void createConfigIfNotExist() {
		try {
			Path dataFolder = this.plugin.getDataFolder().toPath();
			Path path = dataFolder.resolve("config.yml");

			if (Files.notExists(path)) {
				String configVersion = MinecraftVersion.getMajorVersion() + "." + MinecraftVersion.getMinorVersion();

				if (Files.notExists(dataFolder)) {
					Files.createDirectories(dataFolder);
				}

				Files.copy(Orebfuscator.class.getResourceAsStream("/config/config-" + configVersion + ".yml"), path);
			}

			this.systemHash = this.calculateSystemHash(path);
		} catch (IOException e) {
			throw new RuntimeException("unable to create config", e);
		}
	}

	private byte[] calculateSystemHash(Path path) throws IOException {
		return Hashing.murmur3_128().newHasher()
			.putBytes(this.plugin.getDescription().getVersion().getBytes(StandardCharsets.UTF_8))
			.putBytes(MinecraftVersion.getNmsVersion().getBytes(StandardCharsets.UTF_8))
			.putBytes(Files.readAllBytes(path)).hash().asBytes();
	}

	private void deserialize(ConfigurationSection section) {
		if (section.getInt("version", -1) != CONFIG_VERSION) {
			throw new RuntimeException("config is not up to date, please delete your config");
		}

		this.obfuscationConfigs.clear();
		this.proximityConfigs.clear();
		this.worldConfigs.clear();

		ConfigurationSection generalSection = section.getConfigurationSection("general");
		if (generalSection != null) {
			this.generalConfig.deserialize(generalSection);
		} else {
			OFCLogger.warn("config section 'general' is missing, using default one");
		}

		ConfigurationSection cacheSection = section.getConfigurationSection("cache");
		if (cacheSection != null) {
			this.cacheConfig.deserialize(cacheSection);
		} else {
			OFCLogger.warn("config section 'cache' is missing, using default one");
		}

		NmsInstance.close();
		NmsInstance.initialize(this);

		// check if config is still using old path
		String obfuscationConfigPath = section.contains("world") ? "world" : "obfuscation";

		ConfigParser.deserializeSectionList(section, obfuscationConfigPath).stream()
				.map(OrebfuscatorObfuscationConfig::new)
				.forEach(this.obfuscationConfigs::add);
		if (this.obfuscationConfigs.isEmpty()) {
			OFCLogger.warn("config section 'obfuscation' is missing or empty");
		}

		ConfigParser.deserializeSectionList(section, "proximity").stream()
				.map(OrebfuscatorProximityConfig::new)
				.forEach(this.proximityConfigs::add);
		if (this.proximityConfigs.isEmpty()) {
			OFCLogger.warn("config section 'proximity' is missing or empty");
		}

		for (World world : Bukkit.getWorlds()) {
			this.worldConfigs.put(world, new WorldConfigs(world));
		}
	}

	private void serialize(ConfigurationSection section) {
		section.set("version", CONFIG_VERSION);

		this.generalConfig.serialize(section.createSection("general"));
		this.cacheConfig.serialize(section.createSection("cache"));

		List<ConfigurationSection> obfuscationSectionList = new ArrayList<>();
		for (OrebfuscatorObfuscationConfig obfuscationConfig : this.obfuscationConfigs) {
			ConfigurationSection obfuscationSection = new MemoryConfiguration();
			obfuscationConfig.serialize(obfuscationSection);
			obfuscationSectionList.add(obfuscationSection);
		}
		section.set("obfuscation", obfuscationSectionList);

		List<ConfigurationSection> proximitySectionList = new ArrayList<>();
		for (OrebfuscatorProximityConfig proximityConfig : this.proximityConfigs) {
			ConfigurationSection proximitySection = new MemoryConfiguration();
			proximityConfig.serialize(proximitySection);
			proximitySectionList.add(proximitySection);
		}
		section.set("proximity", proximitySectionList);
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
	public BlockFlags blockFlags(World world) {
		return this.getWorldConfigs(world).blockFlags;
	}

	@Override
	public boolean needsObfuscation(World world) {
		return this.getWorldConfigs(world).needsObfuscation;
	}

	@Override
	public ObfuscationConfig obfuscation(World world) {
		return this.getWorldConfigs(world).obfuscationConfig;
	}

	@Override
	public boolean proximityEnabled() {
		for (ProximityConfig proximityConfig : this.proximityConfigs) {
			if (proximityConfig.isEnabled()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ProximityConfig proximity(World world) {
		return this.getWorldConfigs(world).proximityConfig;
	}

	@Override
	public byte[] systemHash() {
		return systemHash;
	}

	public boolean usesFastGaze() {
		for (ProximityConfig config : this.proximityConfigs) {
			if (config.useFastGazeCheck()) {
				return true;
			}
		}
		return false;
	}

	private WorldConfigs getWorldConfigs(World world) {
		this.lock.readLock().lock();
		try {
			WorldConfigs worldConfigs = this.worldConfigs.get(Objects.requireNonNull(world));
			if (worldConfigs != null) {
				return worldConfigs;
			}
		} finally {
			this.lock.readLock().unlock();
		}

		WorldConfigs worldConfigs = new WorldConfigs(world);
		this.lock.writeLock().lock();
		try {
			this.worldConfigs.putIfAbsent(world, worldConfigs);
			return this.worldConfigs.get(world);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	private class WorldConfigs {

		private final OrebfuscatorObfuscationConfig obfuscationConfig;
		private final OrebfuscatorProximityConfig proximityConfig;

		private final OrebfuscatorBlockFlags blockFlags;
		private final boolean needsObfuscation;

		public WorldConfigs(World world) {
			String worldName = world.getName();

			this.obfuscationConfig = findConfig(obfuscationConfigs.stream(), worldName, "obfuscation");
			this.proximityConfig = findConfig(proximityConfigs.stream(), worldName, "proximity");

			this.blockFlags = OrebfuscatorBlockFlags.create(obfuscationConfig, proximityConfig);
			this.needsObfuscation = obfuscationConfig != null && obfuscationConfig.isEnabled() ||
					proximityConfig != null && proximityConfig.isEnabled();
		}

		private <T extends AbstractWorldConfig> T findConfig(Stream<? extends T> configs, String worldName, String configName) {
			List<T> matchingConfigs = configs
					.filter(config -> config.matchesWorldName(worldName))
					.collect(Collectors.toList());

			if (matchingConfigs.size() > 1) {
				OFCLogger.warn(String.format("world '%s' has more than one %s config choosing first one", worldName, configName));
			}

			return matchingConfigs.size() > 0 ? matchingConfigs.get(0) : null;
		}
	}
}
