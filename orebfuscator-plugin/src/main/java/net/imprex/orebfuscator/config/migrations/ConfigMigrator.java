package net.imprex.orebfuscator.config.migrations;

import java.util.HashMap;
import java.util.Map;

import net.imprex.orebfuscator.config.yaml.ConfigurationSection;
import net.imprex.orebfuscator.util.OFCLogger;

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

	public static void migrateToLatestVersion(ConfigurationSection root) {
		while (true) {
			int sourceVersion = root.getInt("version", -1);
			int targetVersion = sourceVersion + 1;

			ConfigMigration migration = MIGRATIONS.get(sourceVersion);
			if (migration == null) {
				break;
			}

			OFCLogger.info("Starting to migrate config to version " + targetVersion);

			root = migration.migrate(root);
			root.set("version", targetVersion);

			OFCLogger.info("Successfully migrated config to version " + targetVersion);
		}
	}
}
