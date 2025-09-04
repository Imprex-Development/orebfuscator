package dev.imprex.orebfuscator.config.migrations;

import java.util.HashMap;
import java.util.Map;

import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.logging.OfcLogger;

public class ConfigMigrator {

  private static final Map<Integer, ConfigMigration> MIGRATIONS = new HashMap<>();

  static {
    register(new ConfigMigrationV1());
    register(new ConfigMigrationV2());
    register(new ConfigMigrationV3());
    register(new ConfigMigrationV4());
  }

  private static void register(ConfigMigration migration) {
    MIGRATIONS.put(migration.sourceVersion(), migration);
  }

  public static boolean willMigrate(ConfigurationSection root) {
    int version = root.getInt("version", -1);
    return MIGRATIONS.get(version) != null;
  }

  public static void migrateToLatestVersion(ConfigurationSection root) {
    while (true) {
      int sourceVersion = root.getInt("version", -1);
      int targetVersion = sourceVersion + 1;

      ConfigMigration migration = MIGRATIONS.get(sourceVersion);
      if (migration == null) {
        break;
      }

      OfcLogger.info("Starting to migrate config to version " + targetVersion);

      root = migration.migrate(root);
      root.set("version", targetVersion);

      OfcLogger.info("Successfully migrated config to version " + targetVersion);
    }
  }
}
