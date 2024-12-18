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
import org.bukkit.plugin.Plugin;

import com.google.common.hash.Hashing;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.config.context.ConfigParsingContext;
import net.imprex.orebfuscator.config.context.DefaultConfigParsingContext;
import net.imprex.orebfuscator.config.migrations.ConfigMigrator;
import net.imprex.orebfuscator.config.yaml.ConfigurationSection;
import net.imprex.orebfuscator.config.yaml.InvalidConfigurationException;
import net.imprex.orebfuscator.config.yaml.YamlConfiguration;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.HeightAccessor;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.WeightedIntRandom;

public class OrebfuscatorConfig implements Config {

	private static final int CONFIG_VERSION = 4;

	private final OrebfuscatorGeneralConfig generalConfig = new OrebfuscatorGeneralConfig();
	private final OrebfuscatorAdvancedConfig advancedConfig = new OrebfuscatorAdvancedConfig();
	private final OrebfuscatorCacheConfig cacheConfig = new OrebfuscatorCacheConfig();

	private final List<OrebfuscatorObfuscationConfig> obfuscationConfigs = new ArrayList<>();
	private final List<OrebfuscatorProximityConfig> proximityConfigs = new ArrayList<>();

	private final Map<World, OrebfuscatorConfig.OrebfuscatorWorldConfigBundle> worldConfigBundles = new WeakHashMap<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private final Plugin plugin;
	private final Path path;
	private final YamlConfiguration configuration;

	private byte[] systemHash;
	private String configReport;

	public OrebfuscatorConfig(Plugin plugin, Path path) {
		this.plugin = plugin;
		this.path = path;
		this.configuration = this.loadConfiguration();
	}

	public YamlConfiguration loadConfiguration() {
		try {
			if (Files.notExists(this.path)) {
				Files.createDirectories(this.path.getParent());

				String configVersion = MinecraftVersion.majorVersion() + "." + MinecraftVersion.minorVersion();
				Files.copy(Orebfuscator.class.getResourceAsStream("/config/config-" + configVersion + ".yml"), path);
			}

			this.systemHash = this.calculateSystemHash(this.path);

			YamlConfiguration configuration = YamlConfiguration.loadConfig(this.path);
			DefaultConfigParsingContext context = new DefaultConfigParsingContext();

			this.deserialize(configuration, context);
			this.configReport = context.report();

			if (context.hasErrors()) {
				throw new IllegalArgumentException("Can't parse config due to errors, Orebfuscator will now disable itself!");
			}

			return configuration;
		} catch (IOException | InvalidConfigurationException e) {
			throw new RuntimeException("Unable to create config", e);
		}
	}

	public void store() {
		for (String path : this.configuration.getKeys(false)) {
			this.configuration.set(path, null);
		}

		this.serialize(this.configuration);

		try {
			this.configuration.save(this.path);
		} catch (IOException e) {
			OFCLogger.error("Can't save config", e);
		}
	}

	private byte[] calculateSystemHash(Path path) throws IOException {
		return Hashing.murmur3_128().newHasher()
			.putBytes(this.plugin.getDescription().getVersion().getBytes(StandardCharsets.UTF_8))
			.putBytes(MinecraftVersion.nmsVersion().getBytes(StandardCharsets.UTF_8))
			.putBytes(Files.readAllBytes(path)).hash().asBytes();
	}

	private void deserialize(ConfigurationSection section, ConfigParsingContext context) {
		ConfigMigrator.migrateToLatestVersion(section);
		// instantly fail on invalid config version
		if (section.getInt("version", -1) != CONFIG_VERSION) {
			throw new RuntimeException("config is not up to date, please delete your config");
		}

		this.obfuscationConfigs.clear();
		this.proximityConfigs.clear();
		this.worldConfigBundles.clear();

		// parse general section
		ConfigParsingContext generalContext = context.section("general");
		ConfigurationSection generalSection = section.getSection("general");
		if (generalSection != null) {
			this.generalConfig.deserialize(generalSection, generalContext);
		} else {
			generalContext.warnMissingSection();
		}

		// parse advanced section
		ConfigParsingContext advancedContext = context.section("advanced");
		ConfigurationSection advancedSection = section.getSection("advanced");
		if (advancedSection != null) {
			this.advancedConfig.deserialize(advancedSection, advancedContext);
		} else {
			advancedContext.warnMissingSection();
		}

		// post init advanced config
		this.advancedConfig.initialize();

		// parse cache section
		ConfigParsingContext cacheContext = context.section("cache", true);
		ConfigurationSection cacheSection = section.getSection("cache");
		if (cacheSection != null) {
			this.cacheConfig.deserialize(cacheSection, cacheContext);
		} else {
			cacheContext.warnMissingSection();
		}

		OrebfuscatorCompatibility.initialize(this.plugin, this);

		OrebfuscatorNms.close();
		OrebfuscatorNms.initialize(this);

		// parse obfuscation sections
		ConfigParsingContext obfuscationContext = context.section("obfuscation");
		ConfigurationSection obfuscationSection = section.getSection("obfuscation");
		if (obfuscationSection != null) {
			for (ConfigurationSection config : obfuscationSection.getSubSections()) {
				ConfigParsingContext configContext = obfuscationContext.section(config.getName(), true);
				this.obfuscationConfigs.add(new OrebfuscatorObfuscationConfig(config, configContext));
			}
		}
		if (this.obfuscationConfigs.isEmpty()) {
			obfuscationContext.warnMissingOrEmpty();
		}

		// parse proximity sections
		ConfigParsingContext proximityContext = context.section("proximity");
		ConfigurationSection proximitySection = section.getSection("proximity");
		if (proximitySection != null) {
			for (ConfigurationSection config : proximitySection.getSubSections()) {
				ConfigParsingContext configContext = proximityContext.section(config.getName(), true);
				this.proximityConfigs.add(new OrebfuscatorProximityConfig(config, configContext));
			}
		}
		if (this.proximityConfigs.isEmpty()) {
			proximityContext.warnMissingOrEmpty();
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
	public byte[] systemHash() {
		return systemHash;
	}

	@Override
	public String report() {
		return configReport;
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
	public WorldConfigBundle world(World world) {
		return this.getWorldConfigBundle(world);
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

	public boolean usesBlockSpecificConfigs() {
		for (OrebfuscatorProximityConfig config : this.proximityConfigs) {
			if (config.usesBlockSpecificConfigs()) {
				return true;
			}
		}
		return false;
	}

	public boolean usesFrustumCulling() {
		for (ProximityConfig config : this.proximityConfigs) {
			if (config.frustumCullingEnabled()) {
				return true;
			}
		}
		return false;
	}

	public String usesRayCastCheck() {
		for (ProximityConfig config : this.proximityConfigs) {
			if (config.rayCastCheckEnabled()) {
				return config.rayCastCheckOnlyCheckCenter() ? "center" : "true";
			}
		}
		return "false";
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
	
		private final HeightAccessor heightAccessor;
		private final WeightedIntRandom[] obfuscationRandoms;
		private final WeightedIntRandom[] proximityRandoms;

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

			this.heightAccessor = HeightAccessor.get(world);
			this.minSectionIndex = heightAccessor.getSectionIndex(this.minY);
			this.maxSectionIndex = heightAccessor.getSectionIndex(this.maxY - 1) + 1;

			this.obfuscationRandoms = this.obfuscationConfig != null
					? this.obfuscationConfig.createWeightedRandoms(heightAccessor) : null;
			this.proximityRandoms = this.proximityConfig != null
					? this.proximityConfig.createWeightedRandoms(heightAccessor) : null;
		}

		private <T extends AbstractWorldConfig> T findConfig(Stream<? extends T> configs, String worldName, String configType) {
			List<T> matchingConfigs = configs
					.filter(config -> config.matchesWorldName(worldName))
					.collect(Collectors.toList());

			if (matchingConfigs.size() > 1) {
				OFCLogger.warn(String.format("world '%s' has more than one %s config choosing first one", worldName, configType));
			}

			T config = matchingConfigs.size() > 0 ? matchingConfigs.get(0) : null;
			String configName = config == null ? "null" : config.getName();

			OFCLogger.debug(String.format("using '%s' %s config for world '%s'", configName, configType, worldName));

			return config;
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
		public boolean needsObfuscation() {
			return this.needsObfuscation;
		}

		@Override
		public boolean skipReadSectionIndex(int index) {
			return index < this.minSectionIndex || index > this.maxSectionIndex;
		}

		@Override
		public boolean skipProcessingSectionIndex(int index) {
			return index < this.minSectionIndex || index > this.maxSectionIndex;
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

		@Override
		public int nextRandomObfuscationBlock(int y) {
			return this.obfuscationRandoms != null
					? this.obfuscationRandoms[y - this.heightAccessor.getMinBuildHeight()].next() : 0;
		}

		@Override
		public int nextRandomProximityBlock(int y) {
			return this.proximityRandoms != null
					? this.proximityRandoms[y - this.heightAccessor.getMinBuildHeight()].next() : 0;
		}
	}
}
