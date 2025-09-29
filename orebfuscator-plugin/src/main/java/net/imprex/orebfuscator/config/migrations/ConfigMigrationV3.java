package net.imprex.orebfuscator.config.migrations;

import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.util.BlockPos;

class ConfigMigrationV3 implements ConfigMigration {

  @Override
  public int sourceVersion() {
    return 3;
  }

  @Override
  public ConfigurationSection migrate(ConfigurationSection section) {
    migrateAdvancedConfig(section.getConfigurationSection("advanced"));
    migrateCacheConfig(section.getConfigurationSection("cache"));
    migrateProximityConfigs(section.getConfigurationSection("proximity"));
    return section;
  }

  private static void migrateAdvancedConfig(ConfigurationSection section) {
    ConfigMigration.migrateNames(section, List.of(
        // obfuscation mapping
        Map.entry("obfuscationWorkerThreads", "obfuscation.threads"),
        Map.entry("obfuscationTimeout", "obfuscation.timeout"),
        Map.entry("maxMillisecondsPerTick", "obfuscation.maxMillisecondsPerTick"),
        // proximity mapping
        Map.entry("proximityHiderThreads", "proximity.threads"),
        Map.entry("proximityDefaultBucketSize", "proximity.defaultBucketSize"),
        Map.entry("proximityThreadCheckInterval", "proximity.threadCheckInterval"),
        Map.entry("proximityPlayerCheckInterval", "proximity.playerCheckInterval")
    ));
  }

  private static void migrateCacheConfig(ConfigurationSection section) {
    ConfigMigration.migrateNames(section, List.of(
        // memory cache mapping
        Map.entry("maximumSize", "memoryCache.maximumSize"),
        Map.entry("expireAfterAccess", "memoryCache.expireAfterAccess"),
        // disk cache mapping
        Map.entry("enableDiskCache", "diskCache.enabled"),
        Map.entry("baseDirectory", "diskCache.directory"),
        Map.entry("maximumOpenRegionFiles", "diskCache.maximumOpenFiles"),
        Map.entry("deleteRegionFilesAfterAccess", "diskCache.deleteFilesAfterAccess"),
        Map.entry("maximumTaskQueueSize", "diskCache.maximumTaskQueueSize")
    ));
  }

  private static void migrateProximityConfigs(ConfigurationSection parentSection) {
    for (String key : parentSection.getKeys(false)) {
      ConfigurationSection section = parentSection.getConfigurationSection(key);

      // LEGACY: transform to post 5.2.2
      if (section.isConfigurationSection("defaults")) {
        int y = section.getInt("defaults.y");
        if (section.getBoolean("defaults.above")) {
          section.set("minY", y);
          section.set("maxY", BlockPos.MAX_Y);
        } else {
          section.set("minY", BlockPos.MIN_Y);
          section.set("maxY", y);
        }

        section.set("useBlockBelow", section.getBoolean("defaults.useBlockBelow"));
      }

      // rename all ray cast name variations
      if (section.isBoolean("useRayCastCheck") || section.isBoolean("useFastGazeCheck")) {
        section.set("rayCastCheck.enabled",
            section.getBoolean("useRayCastCheck",
                section.getBoolean("useFastGazeCheck")));
      }

      // LEGACY: transform pre 5.2.2 height condition
      ConfigurationSection blockSection = section.getConfigurationSection("hiddenBlocks");
      for (String blockName : blockSection.getKeys(false)) {

        if (blockSection.isInt(blockName + ".y") && blockSection.isBoolean(blockName + ".above")) {
          int y = blockSection.getInt(blockName + ".y");
          if (blockSection.getBoolean(blockName + ".above")) {
            blockSection.set(blockName + ".minY", y);
            blockSection.set(blockName + ".maxY", BlockPos.MAX_Y);
          } else {
            blockSection.set(blockName + ".minY", BlockPos.MIN_Y);
            blockSection.set(blockName + ".maxY", y);
          }
        }
      }
    }
  }
}
