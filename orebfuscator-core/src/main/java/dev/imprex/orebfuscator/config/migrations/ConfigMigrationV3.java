package dev.imprex.orebfuscator.config.migrations;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.util.BlockPos;

class ConfigMigrationV3 implements ConfigMigration {

  @Override
  public int sourceVersion() {
    return 3;
  }

  @Override
  public @NotNull ConfigurationSection migrate(@NotNull ConfigurationSection root) {
    migrateAdvancedConfig(root);
    migrateCacheConfig(root);
    migrateProximityConfigs(root);
    return root;
  }

  private static void migrateAdvancedConfig(ConfigurationSection root) {
    ConfigMigration.migrateNames(root.getSection("advanced"), List.of(
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

  private static void migrateCacheConfig(ConfigurationSection root) {
    ConfigMigration.migrateNames(root.getSection("cache"), List.of(
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

  private static void migrateProximityConfigs(ConfigurationSection root) {
    ConfigurationSection configContainer = root.getSection("proximity");
    if (configContainer == null) {
      return;
    }

    for (ConfigurationSection config : configContainer.getSubSections()) {
      // LEGACY: transform to post 5.2.2
      if (config.isSection("defaults")) {
        Integer y = config.getInt("defaults.y");
        if (config.getBoolean("defaults.above", false)) {
          config.set("minY", y);
          config.set("maxY", BlockPos.MAX_Y);
        } else {
          config.set("minY", BlockPos.MIN_Y);
          config.set("maxY", y);
        }

        config.set("useBlockBelow", config.getBoolean("defaults.useBlockBelow"));
      }

      // rename all ray cast name variations
      if (config.isBoolean("useRayCastCheck") || config.isBoolean("useFastGazeCheck")) {
        config.set("rayCastCheck.enabled",
            config.getBoolean("useRayCastCheck",
                config.getBoolean("useFastGazeCheck")));
      }

      // LEGACY: transform pre 5.2.2 height condition
      ConfigurationSection hiddenBlocks = config.getSection("hiddenBlocks");
      if (hiddenBlocks == null) {
        continue;
      }

      for (ConfigurationSection block : hiddenBlocks.getSubSections()) {
        if (block.isInt("y") && block.isBoolean("above")) {
          Integer y = block.getInt("y");
          if (block.getBoolean("above", false)) {
            block.set("minY", y);
            block.set("maxY", BlockPos.MAX_Y);
          } else {
            block.set("minY", BlockPos.MIN_Y);
            block.set("maxY", y);
          }
        }
      }
    }
  }
}
