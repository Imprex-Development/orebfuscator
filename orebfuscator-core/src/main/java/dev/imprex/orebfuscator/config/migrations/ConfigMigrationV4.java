package dev.imprex.orebfuscator.config.migrations;

import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class ConfigMigrationV4 implements ConfigMigration {

  @Override
  public int sourceVersion() {
    return 4;
  }

  @Override
  public ConfigurationSection migrate(ConfigurationSection root) {
    migrateWorlds(root.getSection("obfuscation"));
    migrateWorlds(root.getSection("proximity"));
    return root;
  }

  private static void migrateWorlds(@Nullable ConfigurationSection configContainer) {
    if (configContainer == null) {
      return;
    }

    for (ConfigurationSection config : configContainer.getSubSections()) {
      var worlds = config.getStringList("worlds").stream().map(value -> {
        if (value.startsWith("regex:")) {
          return String.format("regex(%s)", value.substring(6));
        }
        return value;
      }).toList();

      config.set("worlds", worlds);
    }
  }
}
