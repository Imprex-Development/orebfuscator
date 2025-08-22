package dev.imprex.orebfuscator.config.migrations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;

class ConfigMigrationV1 implements ConfigMigration {

  @Override
  public int sourceVersion() {
    return 1;
  }

  @Override
  public @NotNull ConfigurationSection migrate(@NotNull ConfigurationSection root) {
    // check if config is still using old path
    String obfuscationConfigPath = root.contains("world") ? "world" : "obfuscation";
    convertSectionListToSection(root, obfuscationConfigPath);
    convertSectionListToSection(root, "proximity");

    return root;
  }

  private static void convertSectionListToSection(ConfigurationSection root, String path) {
    List<ConfigurationSection> configList = getSectionList(root, path);
    ConfigurationSection configContainer = root.createSection(path);

    for (ConfigurationSection config : configList) {
      configContainer.set(config.getName(), config);
    }
  }

  private static List<ConfigurationSection> getSectionList(ConfigurationSection root, String path) {
    List<ConfigurationSection> configList = new ArrayList<>();

    List<?> list = root.getList(path, Collections.emptyList());
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i) instanceof Map<?, ?> map) {
        ConfigurationSection config = root.createSection(path + "-" + i);
        configList.add(convertMapsToSections(map, config));
      }
    }

    return configList;
  }

  private static ConfigurationSection convertMapsToSections(Map<?, ?> entries, ConfigurationSection section) {
    for (Map.Entry<?, ?> entry : entries.entrySet()) {
      String key = entry.getKey().toString();
      Object value = entry.getValue();

      if (value instanceof Map<?, ?> map) {
        convertMapsToSections(map, section.createSection(key));
      } else {
        section.set(key, value);
      }
    }
    return section;
  }
}
