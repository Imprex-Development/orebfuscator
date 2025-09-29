package dev.imprex.orebfuscator.config.migrations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

class ConfigMigrationV1 implements ConfigMigration {

  @Override
  public int sourceVersion() {
    return 1;
  }

  @Override
  public ConfigurationSection migrate(ConfigurationSection section) {
    // check if config is still using old path
    String obfuscationConfigPath = section.contains("world") ? "world" : "obfuscation";
    this.convertSectionListToSection(section, obfuscationConfigPath);
    this.convertSectionListToSection(section, "proximity");

    return section;
  }

  private void convertSectionListToSection(ConfigurationSection parentSection, String path) {
    List<ConfigurationSection> sections = this.deserializeSectionList(parentSection, path);
    ConfigurationSection section = parentSection.createSection(path);
    for (ConfigurationSection childSection : sections) {
      section.set(childSection.getName(), childSection);
    }
  }

  private List<ConfigurationSection> deserializeSectionList(ConfigurationSection parentSection, String path) {
    List<ConfigurationSection> sections = new ArrayList<>();

    List<?> sectionList = parentSection.getList(path);
    if (sectionList != null) {
      for (int i = 0; i < sectionList.size(); i++) {
        Object section = sectionList.get(i);
        if (section instanceof Map) {
          sections.add(this.convertMapsToSections((Map<?, ?>) section,
              parentSection.createSection(path + "-" + i)));
        }
      }
    }

    return sections;
  }

  private ConfigurationSection convertMapsToSections(Map<?, ?> input, ConfigurationSection section) {
    for (Map.Entry<?, ?> entry : input.entrySet()) {
      String key = entry.getKey().toString();
      Object value = entry.getValue();

      if (value instanceof Map) {
        this.convertMapsToSections((Map<?, ?>) value, section.createSection(key));
      } else {
        section.set(key, value);
      }
    }
    return section;
  }
}
