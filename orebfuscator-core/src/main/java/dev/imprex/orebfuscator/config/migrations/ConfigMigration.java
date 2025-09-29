package dev.imprex.orebfuscator.config.migrations;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;

interface ConfigMigration {

  int sourceVersion();

  @NotNull ConfigurationSection migrate(@NotNull ConfigurationSection root);

  static void migrateNames(@Nullable ConfigurationSection section, @NotNull List<Map.Entry<String, String>> mapping) {
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
