package dev.imprex.orebfuscator.config.migrations;

import java.util.List;
import java.util.Map;
import java.util.Objects;


import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
interface ConfigMigration {

  int sourceVersion();

  ConfigurationSection migrate(ConfigurationSection root);

  static void migrateNames(@Nullable ConfigurationSection section, List<Map.Entry<String, String>> mapping) {
    Objects.requireNonNull(mapping, "mappings can't be null");
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
