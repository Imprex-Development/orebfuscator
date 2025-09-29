package dev.imprex.orebfuscator.config.migrations;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

import dev.imprex.orebfuscator.logging.OfcLogger;

public class ConfigMigrator {

  private static final Map<Integer, ConfigMigration> MIGRATIONS = new HashMap<>();

  static {
    register(new ConfigMigrationV1());
    register(new ConfigMigrationV2());
    register(new ConfigMigrationV3());
  }

  private static void register(ConfigMigration migration) {
    MIGRATIONS.put(migration.sourceVersion(), migration);
  }

  public static void migrateToLatestVersion(ConfigurationSection section) {
    while (true) {
      int sourceVersion = section.getInt("version", -1);
      int targetVersion = sourceVersion + 1;

      ConfigMigration migration = MIGRATIONS.get(sourceVersion);
      if (migration == null) {
        break;
      }

      OfcLogger.info("Starting to migrate config to version " + targetVersion);

      section = migration.migrate(section);
      section.set("version", targetVersion);

      OfcLogger.info("Successfully migrated config to version " + targetVersion);
    }
  }
}
