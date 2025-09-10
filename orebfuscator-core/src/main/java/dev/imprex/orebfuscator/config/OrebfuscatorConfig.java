package dev.imprex.orebfuscator.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.google.common.hash.Hashing;
import dev.imprex.orebfuscator.config.api.AdvancedConfig;
import dev.imprex.orebfuscator.config.api.BlockFlags;
import dev.imprex.orebfuscator.config.api.CacheConfig;
import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.config.api.GeneralConfig;
import dev.imprex.orebfuscator.config.api.ObfuscationConfig;
import dev.imprex.orebfuscator.config.api.ProximityConfig;
import dev.imprex.orebfuscator.config.api.WorldConfigBundle;
import dev.imprex.orebfuscator.config.components.BlockParser;
import dev.imprex.orebfuscator.config.context.ConfigMessage;
import dev.imprex.orebfuscator.config.context.ConfigParsingContext;
import dev.imprex.orebfuscator.config.context.DefaultConfigParsingContext;
import dev.imprex.orebfuscator.config.migrations.ConfigMigrator;
import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.config.yaml.InvalidConfigurationException;
import dev.imprex.orebfuscator.config.yaml.YamlConfiguration;
import dev.imprex.orebfuscator.interop.ServerAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.Version;
import dev.imprex.orebfuscator.util.WeightedRandom;

public class OrebfuscatorConfig implements Config {

  private static final int CONFIG_VERSION = 5;

  private final OrebfuscatorGeneralConfig generalConfig = new OrebfuscatorGeneralConfig();
  private final OrebfuscatorAdvancedConfig advancedConfig = new OrebfuscatorAdvancedConfig();
  private final OrebfuscatorCacheConfig cacheConfig;

  private final List<OrebfuscatorObfuscationConfig> obfuscationConfigs = new ArrayList<>();
  private final List<OrebfuscatorProximityConfig> proximityConfigs = new ArrayList<>();

  private final Map<WorldAccessor, OrebfuscatorConfig.OrebfuscatorWorldConfigBundle> worldConfigBundles = new WeakHashMap<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private final ServerAccessor server;
  private final Path path;
  private final YamlConfiguration configuration;

  private byte[] systemHash;
  private String configReport;

  public OrebfuscatorConfig(ServerAccessor server) {
    this.server = server;
    this.path = server.getConfigDirectory().resolve("config.yml");

    this.cacheConfig = new OrebfuscatorCacheConfig(this.server);
    this.configuration = this.loadConfiguration();
  }

  public YamlConfiguration loadConfiguration() {
    try {
      // try to create default config if necessary
      if (Files.notExists(this.path)) {
        Files.createDirectories(this.path.getParent());

        Version version = this.server.getMinecraftVersion();
        InputStream inputStream = OrebfuscatorConfig.class.getResourceAsStream(
            "/config/config-" + version + ".yml");

        // check for prefix if full version doesn't match
        if (inputStream == null) {
          String configVersion = version.major() + "." + version.minor();
          inputStream = OrebfuscatorConfig.class.getResourceAsStream(
              "/config/config-" + configVersion + ".yml");
        }

        try (InputStream stream = Objects.requireNonNull(inputStream,
            "Can't find default config for version: " + version)) {
          Files.copy(stream, path);
        }
      }

      YamlConfiguration configuration = YamlConfiguration.loadConfig(this.path);
      DefaultConfigParsingContext context = new DefaultConfigParsingContext();

      this.deserialize(configuration, context);
      this.configReport = context.report();

      if (context.hasErrors()) {
        OfcLogger.error(this.configReport, null);
        throw new IllegalArgumentException(
            "Can't parse config due to errors, Orebfuscator will now disable itself!");
      } else if (this.configReport != null) {
        OfcLogger.warn(this.configReport);
      }

      this.systemHash = this.calculateSystemHash(configuration);

      return configuration;
    } catch (IOException | InvalidConfigurationException e) {
      throw new RuntimeException("Unable to create/read config", e);
    }
  }

  public void store() {
    this.configuration.clear();
    this.serialize(this.configuration);

    try {
      this.configuration.save(this.path);
    } catch (IOException e) {
      OfcLogger.error("Can't save config", e);
    }
  }

  private byte[] calculateSystemHash(YamlConfiguration configuration) throws IOException {
    return Hashing.murmur3_128().newHasher()
        .putBytes(this.server.getOrebfuscatorVersion().getBytes(StandardCharsets.UTF_8))
        .putBytes(this.server.getMinecraftVersion().toString().getBytes(StandardCharsets.UTF_8))
        .putBytes(configuration.withoutComments().getBytes(StandardCharsets.UTF_8))
        .hash().asBytes();
  }

  private void deserialize(YamlConfiguration configuration, ConfigParsingContext context) {
    if (ConfigMigrator.willMigrate(configuration)) {
      try {
        configuration.save(server.getConfigDirectory().resolve("config-old.yml"));
      } catch (IOException e) {
        OfcLogger.error("Can't save original config before migration", e);
      }
    }

    // try to migrate to latest version
    ConfigMigrator.migrateToLatestVersion(configuration);

    // instantly fail on invalid config version
    if (configuration.getInt("version", -1) != CONFIG_VERSION) {
      throw new RuntimeException("config is not up to date, please delete your config");
    }

    this.obfuscationConfigs.clear();
    this.proximityConfigs.clear();
    this.worldConfigBundles.clear();

    // parse general section
    ConfigParsingContext generalContext = context.section("general");
    ConfigurationSection generalSection = configuration.getSection("general");
    if (generalSection != null) {
      this.generalConfig.deserialize(generalSection, generalContext);
    } else {
      generalContext.warn(ConfigMessage.MISSING_USING_DEFAULTS);
    }

    // parse advanced section
    ConfigParsingContext advancedContext = context.section("advanced");
    ConfigurationSection advancedSection = configuration.getSection("advanced");
    if (advancedSection != null) {
      this.advancedConfig.deserialize(advancedSection, advancedContext);
    } else {
      advancedContext.warn(ConfigMessage.MISSING_USING_DEFAULTS);
    }

    // parse cache section
    ConfigParsingContext cacheContext = context.section("cache", true);
    ConfigurationSection cacheSection = configuration.getSection("cache");
    if (cacheSection != null) {
      this.cacheConfig.deserialize(cacheSection, cacheContext);
    } else {
      cacheContext.warn(ConfigMessage.MISSING_USING_DEFAULTS);
    }

    final BlockParser.Factory blockParserFactory = BlockParser.factory(server.getRegistry());

    // parse obfuscation sections
    ConfigParsingContext obfuscationContext = context.section("obfuscation");
    ConfigurationSection obfuscationSection = configuration.getSection("obfuscation");
    if (obfuscationSection != null) {
      for (ConfigurationSection config : obfuscationSection.getSubSections()) {
        ConfigParsingContext configContext = obfuscationContext.section(config.getName(), true);
        this.obfuscationConfigs.add(
            new OrebfuscatorObfuscationConfig(blockParserFactory, config, configContext));
      }
    }
    if (this.obfuscationConfigs.isEmpty()) {
      obfuscationContext.warn(ConfigMessage.MISSING_OR_EMPTY);
    }

    // parse proximity sections
    ConfigParsingContext proximityContext = context.section("proximity");
    ConfigurationSection proximitySection = configuration.getSection("proximity");
    if (proximitySection != null) {
      for (ConfigurationSection config : proximitySection.getSubSections()) {
        ConfigParsingContext configContext = proximityContext.section(config.getName(), true);
        this.proximityConfigs.add(new OrebfuscatorProximityConfig(blockParserFactory, config, configContext));
      }
    }
    if (this.proximityConfigs.isEmpty()) {
      proximityContext.warn(ConfigMessage.MISSING_OR_EMPTY);
    }

    for (WorldAccessor world : this.server.getWorlds()) {
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
  public WorldConfigBundle world(WorldAccessor world) {
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

  private OrebfuscatorWorldConfigBundle getWorldConfigBundle(WorldAccessor world) {
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

    private final BlockFlags blockFlags;
    private final boolean needsObfuscation;

    private final int minY;
    private final int maxY;

    private final int minSectionIndex;
    private final int maxSectionIndex;

    private final WorldAccessor world;
    private final WeightedRandom[] obfuscationRandoms;
    private final WeightedRandom[] proximityRandoms;

    public OrebfuscatorWorldConfigBundle(WorldAccessor world) {
      String worldName = world.getName();
      this.world = world;

      this.obfuscationConfig = findConfig(obfuscationConfigs, worldName, "obfuscation");
      this.proximityConfig = findConfig(proximityConfigs, worldName, "proximity");

      this.blockFlags = OrebfuscatorBlockFlags.create(server.getRegistry(), obfuscationConfig, proximityConfig);
      this.needsObfuscation = obfuscationConfig != null && obfuscationConfig.isEnabled() ||
          proximityConfig != null && proximityConfig.isEnabled();

      this.minY = Math.min(
          this.obfuscationConfig != null ? this.obfuscationConfig.getMinY() : BlockPos.MAX_Y,
          this.proximityConfig != null ? this.proximityConfig.getMinY() : BlockPos.MAX_Y);
      this.maxY = Math.max(
          this.obfuscationConfig != null ? this.obfuscationConfig.getMaxY() : BlockPos.MIN_Y,
          this.proximityConfig != null ? this.proximityConfig.getMaxY() : BlockPos.MIN_Y);

      this.minSectionIndex = world.getSectionIndex(this.minY);
      this.maxSectionIndex = world.getSectionIndex(this.maxY - 1) + 1;

      this.obfuscationRandoms = this.obfuscationConfig != null
          ? this.obfuscationConfig.createWeightedRandoms(world) : null;
      this.proximityRandoms = this.proximityConfig != null
          ? this.proximityConfig.createWeightedRandoms(world) : null;
    }

    private <T extends AbstractWorldConfig> T findConfig(Collection<T> configs, String worldName,
        String configType) {
      List<T> matchingConfigs = configs.stream()
          .filter(config -> config.matchesWorldName(worldName))
          .toList();

      if (matchingConfigs.size() > 1) {
        OfcLogger.warn(String.format("world '%s' has more than one %s config choosing first one", worldName,
            configType));
      }

      T config = !matchingConfigs.isEmpty() ? matchingConfigs.get(0) : null;
      String configName = config == null ? "null" : config.getName();

      OfcLogger.debug(String.format("using '%s' %s config for world '%s'", configName, configType, worldName));

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
          ? this.obfuscationRandoms[y - this.world.getMinBuildHeight()].next() : 0;
    }

    @Override
    public int nextRandomProximityBlock(int y) {
      return this.proximityRandoms != null
          ? this.proximityRandoms[y - this.world.getMinBuildHeight()].next() : 0;
    }
  }
}
