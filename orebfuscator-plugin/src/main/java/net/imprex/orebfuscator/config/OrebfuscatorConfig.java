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
import org.bukkit.plugin.Plugin;

import com.google.common.hash.Hashing;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.HeightAccessor;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorConfig implements Config {

	private static final int CONFIG_VERSION = 2;

	private final OrebfuscatorGeneralConfig generalConfig = new OrebfuscatorGeneralConfig();
	private final OrebfuscatorAdvancedConfig advancedConfig = new OrebfuscatorAdvancedConfig();
	private final OrebfuscatorCacheConfig cacheConfig = new OrebfuscatorCacheConfig();

	private final List<OrebfuscatorObfuscationConfig> obfuscationConfigs = new ArrayList<>();
	private final List<OrebfuscatorProximityConfig> proximityConfigs = new ArrayList<>();

	private final Map<World, OrebfuscatorConfig.OrebfuscatorWorldConfigBundle> worldConfigBundles = new WeakHashMap<>();
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
		if (section.getInt("version", -1) == 1) {
			// check if config is still using old path MemoryConfiguration
			String obfuscationConfigPath = section.contains("world") ? "world" : "obfuscation";
			ConfigParser.convertSectionListToSection(section, obfuscationConfigPath);
			ConfigParser.convertSectionListToSection(section, "proximity");
			section.set("version", CONFIG_VERSION);
		}

		if (section.getInt("version", -1) != CONFIG_VERSION) {
			throw new RuntimeException("config is not up to date, please delete your config");
		}

		this.obfuscationConfigs.clear();
		this.proximityConfigs.clear();
		this.worldConfigBundles.clear();

		ConfigurationSection generalSection = section.getConfigurationSection("general");
		if (generalSection != null) {
			this.generalConfig.deserialize(generalSection);
		} else {
			OFCLogger.warn("config section 'general' is missing, using default one");
		}

		ConfigurationSection advancedSection = section.getConfigurationSection("advanced");
		if (advancedSection != null) {
			this.advancedConfig.deserialize(advancedSection);
		} else {
			OFCLogger.warn("config section 'advanced' is missing, using default one");
		}

		this.advancedConfig.initialize();

		ConfigurationSection cacheSection = section.getConfigurationSection("cache");
		if (cacheSection != null) {
			this.cacheConfig.deserialize(cacheSection);
		} else {
			OFCLogger.warn("config section 'cache' is missing, using default one");
		}

		NmsInstance.close();
		NmsInstance.initialize(this);

		ConfigurationSection obfuscation = section.getConfigurationSection("obfuscation");
		obfuscation.getKeys(false).stream()
				.map(obfuscation::getConfigurationSection)
				.map(OrebfuscatorObfuscationConfig::new)
				.forEach(this.obfuscationConfigs::add);
		if (this.obfuscationConfigs.isEmpty()) {
			OFCLogger.warn("config section 'obfuscation' is missing or empty");
		}

		ConfigurationSection proximity = section.getConfigurationSection("proximity");
		proximity.getKeys(false).stream()
				.map(proximity::getConfigurationSection)
				.map(OrebfuscatorProximityConfig::new)
				.forEach(this.proximityConfigs::add);
		if (this.proximityConfigs.isEmpty()) {
			OFCLogger.warn("config section 'proximity' is missing or empty");
		}

		for (World world : Bukkit.getWorlds()) {
			this.worldConfigBundles.put(world, new OrebfuscatorWorldConfigBundle(world));
		}
	}

	private void serialize(ConfigurationSection section) {
		section.set("version", CONFIG_VERSION);

		this.generalConfig.serialize(section.createSection("general"));
		this.advancedConfig.serialize(section.createSection("advanced"));
		this.cacheConfig.serialize(section.createSection("cache"));

		ConfigurationSection obfuscation = section.createSection("obfuscation");
		for (OrebfuscatorObfuscationConfig obfuscationConfig : this.obfuscationConfigs) {
			obfuscationConfig.serialize(obfuscation.createSection(obfuscationConfig.getName()));
		}

		ConfigurationSection proximity = section.createSection("proximity");
		for (OrebfuscatorProximityConfig proximityConfig : this.proximityConfigs) {
			proximityConfig.serialize(proximity.createSection(proximityConfig.getName()));
		}
	}

	@Override
	public GeneralConfig general() {
		return this.generalConfig;
	}

	@Override
	public AdvancedConfig advanced() {
		return this.advancedConfig;
	}

	@Override
	public CacheConfig cache() {
		return this.cacheConfig;
	}

	@Override
	public WorldConfigBundle bundle(World world) {
		return this.getWorldConfigBundle(world);
	}

	@Override
	public BlockFlags blockFlags(World world) {
		return this.getWorldConfigBundle(world).blockFlags;
	}

	@Override
	public boolean needsObfuscation(World world) {
		return this.getWorldConfigBundle(world).needsObfuscation;
	}

	@Override
	public ObfuscationConfig obfuscation(World world) {
		return this.getWorldConfigBundle(world).obfuscationConfig;
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
		return this.getWorldConfigBundle(world).proximityConfig;
	}

	@Override
	public byte[] systemHash() {
		return systemHash;
	}

	public boolean usesBlockSpecificConfigs() {
		for (OrebfuscatorProximityConfig config : this.proximityConfigs) {
			if (config.usesBlockSpecificConfigs()) {
				return true;
			}
		}
		return false;
	}

	public boolean usesFastGaze() {
		for (ProximityConfig config : this.proximityConfigs) {
			if (config.useFastGazeCheck()) {
				return true;
			}
		}
		return false;
	}

	private OrebfuscatorWorldConfigBundle getWorldConfigBundle(World world) {
		this.lock.readLock().lock();
		try {
			OrebfuscatorWorldConfigBundle worldConfigs = this.worldConfigBundles.get(Objects.requireNonNull(world));
			if (worldConfigs != null) {
				return worldConfigs;
			}
		} finally {
			this.lock.readLock().unlock();
		}

		OrebfuscatorWorldConfigBundle worldConfigs = new OrebfuscatorWorldConfigBundle(world);
		this.lock.writeLock().lock();
		try {
			this.worldConfigBundles.putIfAbsent(world, worldConfigs);
			return this.worldConfigBundles.get(world);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	private class OrebfuscatorWorldConfigBundle implements WorldConfigBundle {

		private final OrebfuscatorObfuscationConfig obfuscationConfig;
		private final OrebfuscatorProximityConfig proximityConfig;

		private final OrebfuscatorBlockFlags blockFlags;
		private final boolean needsObfuscation;

		private final int minY;
		private final int maxY;

		private final int minSectionIndex;
		private final int maxSectionIndex;

		public OrebfuscatorWorldConfigBundle(World world) {
			String worldName = world.getName();

			this.obfuscationConfig = findConfig(obfuscationConfigs.stream(), worldName, "obfuscation");
			this.proximityConfig = findConfig(proximityConfigs.stream(), worldName, "proximity");

			this.blockFlags = OrebfuscatorBlockFlags.create(obfuscationConfig, proximityConfig);
			this.needsObfuscation = obfuscationConfig != null && obfuscationConfig.isEnabled() ||
					proximityConfig != null && proximityConfig.isEnabled();

			this.minY = Math.min(
					this.obfuscationConfig != null ? this.obfuscationConfig.getMinY() : BlockPos.MAX_Y,
					this.proximityConfig != null ? this.proximityConfig.getMinY() : BlockPos.MAX_Y);
			this.maxY = Math.max(
					this.obfuscationConfig != null ? this.obfuscationConfig.getMaxY() : BlockPos.MIN_Y,
					this.proximityConfig != null ? this.proximityConfig.getMaxY() : BlockPos.MIN_Y);

			HeightAccessor heightAccessor = HeightAccessor.get(world);
			this.minSectionIndex = heightAccessor.getSectionIndex(this.minY);
			this.maxSectionIndex = heightAccessor.getSectionIndex(this.maxY - 1) + 1;
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

		@Override
		public BlockFlags blockFlags() {
			return this.blockFlags;
		}

		@Override
		public ObfuscationConfig obfuscation() {
			return this.obfuscationConfig;
		}

		@Override
		public ProximityConfig proximity() {
			return this.proximityConfig;
		}

		@Override
		public int minSectionIndex() {
			return this.minSectionIndex;
		}

		@Override
		public int maxSectionIndex() {
			return this.maxSectionIndex;
		}

		@Override
		public boolean shouldObfuscate(int y) {
			return y >= this.minY && y <= this.maxY;
		}
	}
}
