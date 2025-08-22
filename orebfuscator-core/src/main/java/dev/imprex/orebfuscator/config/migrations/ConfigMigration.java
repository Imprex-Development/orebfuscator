package dev.imprex.orebfuscator.config.migrations;

import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

interface ConfigMigration {

	int sourceVersion();

	ConfigurationSection migrate(ConfigurationSection section);

	static void migrateNames(ConfigurationSection section, List<Map.Entry<String, String>> mapping) {
		if (section == null) {
			return;
		}

		for (Map.Entry<String, String> entry : mapping) {
			Object value = section.get(entry.getKey());
			if (value != null) {
				section.set(entry.getValue(), value);
			}
		}
	}
}
