package dev.imprex.orebfuscator.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import dev.imprex.orebfuscator.config.api.CacheConfig;
import dev.imprex.orebfuscator.config.context.ConfigMessage;
import dev.imprex.orebfuscator.config.context.ConfigParsingContext;
import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.interop.ServerAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.util.ChunkCacheKey;

public class OrebfuscatorCacheConfig implements CacheConfig {

  private final Path worldDirectory;

  private boolean enabledValue = true;
  private int maximumSize = 32768;
  private long expireAfterAccess = TimeUnit.SECONDS.toMillis(60);

  private boolean enableDiskCacheValue = true;
  private Path baseDirectory;
  private int maximumOpenRegionFiles = 256;
  private long deleteRegionFilesAfterAccess = TimeUnit.DAYS.toMillis(2);
  private int maximumTaskQueueSize = 32768;

  // feature enabled states after context evaluation
  private boolean enabled = false;
  private boolean enableDiskCache = false;

  public OrebfuscatorCacheConfig(ServerAccessor server) {
    this.worldDirectory = server.getWorldDirectory().normalize();
    this.baseDirectory = this.worldDirectory.resolve("orebfuscator_cache/");
  }

  public void deserialize(ConfigurationSection section, ConfigParsingContext context) {
    this.enabledValue = section.getBoolean("enabled", true);

    // parse memoryCache section
    ConfigParsingContext memoryContext = context.section("memoryCache");
    ConfigurationSection memorySection = section.getSection("memoryCache");
    if (memorySection != null) {
      this.maximumSize = memorySection.getInt("maximumSize", 32768);
      memoryContext.errorMinValue("maximumSize", 1, this.maximumSize);

      this.expireAfterAccess = memorySection.getLong("expireAfterAccess", TimeUnit.SECONDS.toMillis(60));
      memoryContext.errorMinValue("expireAfterAccess", 1, this.expireAfterAccess);
    } else {
      memoryContext.warn(ConfigMessage.MISSING_USING_DEFAULTS);
    }

    // parse diskCache section, isolate errors to disable only diskCache on section error
    ConfigParsingContext diskContext = context.section("diskCache", true);
    ConfigurationSection diskSection = section.getSection("diskCache");
    if (diskSection != null) {
      this.enableDiskCacheValue = diskSection.getBoolean("enabled", true);
      this.baseDirectory = this.deserializeBaseDirectory(diskSection, diskContext, "orebfuscator_cache/");

      this.maximumOpenRegionFiles = diskSection.getInt("maximumOpenFiles", 256);
      diskContext.errorMinValue("maximumOpenFiles", 1, this.maximumOpenRegionFiles);

      this.deleteRegionFilesAfterAccess = diskSection.getLong("deleteFilesAfterAccess",
          TimeUnit.DAYS.toMillis(2));
      diskContext.errorMinValue("deleteFilesAfterAccess", 1, this.deleteRegionFilesAfterAccess);

      this.maximumTaskQueueSize = diskSection.getInt("maximumTaskQueueSize", 32768);
      diskContext.errorMinValue("maximumTaskQueueSize", 1, this.maximumTaskQueueSize);
    } else {
      diskContext.warn(ConfigMessage.MISSING_USING_DEFAULTS);
    }

    // try to create diskCache.directory
    if (this.enabledValue && this.enableDiskCacheValue) {
      OfcLogger.debug("Using '" + this.baseDirectory.toAbsolutePath() + "' as chunk cache path");
      try {
        if (Files.notExists(this.baseDirectory)) {
          Files.createDirectories(this.baseDirectory);
        }
      } catch (IOException e) {
        diskContext.error(ConfigMessage.CACHE_CAN_NOT_CREATE, e);
      }
    }

    // disable features if their config sections contain errors
    this.enabled = context.disableIfError(this.enabledValue);
    this.enableDiskCache = diskContext.disableIfError(this.enableDiskCacheValue);
  }

  public void serialize(ConfigurationSection section) {
    section.set("enabled", this.enabledValue);

    section.set("memoryCache.maximumSize", this.maximumSize);
    section.set("memoryCache.expireAfterAccess", this.expireAfterAccess);

    section.set("diskCache.enabled", this.enableDiskCacheValue);

    String directoryString = this.worldDirectory.relativize(baseDirectory).toString();
    section.set("diskCache.directory", directoryString);

    section.set("diskCache.maximumOpenFiles", this.maximumOpenRegionFiles);
    section.set("diskCache.deleteFilesAfterAccess", this.deleteRegionFilesAfterAccess);
    section.set("diskCache.maximumTaskQueueSize", this.maximumTaskQueueSize);
  }

  private Path deserializeBaseDirectory(ConfigurationSection section, ConfigParsingContext context,
      String defaultPath) {
    String baseDirectory = section.getString("directory", defaultPath);

    try {
      return this.worldDirectory.resolve(baseDirectory).normalize();
    } catch (InvalidPathException e) {
      context.warn("directory", ConfigMessage.CACHE_INVALID_PATH, baseDirectory, defaultPath);
      return this.worldDirectory.resolve(defaultPath).normalize();
    }
  }

  @Override
  public boolean enabled() {
    return this.enabled;
  }

  @Override
  public int maximumSize() {
    return this.maximumSize;
  }

  @Override
  public long expireAfterAccess() {
    return this.expireAfterAccess;
  }

  @Override
  public boolean enableDiskCache() {
    return this.enableDiskCache;
  }

  @Override
  public Path baseDirectory() {
    return this.baseDirectory;
  }

  @Override
  public Path regionFile(ChunkCacheKey key) {
    return this.baseDirectory.resolve(key.world())
        .resolve("r." + (key.x() >> 5) + "." + (key.z() >> 5) + ".mca");
  }

  @Override
  public int maximumOpenRegionFiles() {
    return this.maximumOpenRegionFiles;
  }

  @Override
  public long deleteRegionFilesAfterAccess() {
    return this.deleteRegionFilesAfterAccess;
  }

  @Override
  public int maximumTaskQueueSize() {
    return this.maximumTaskQueueSize;
  }
}
