package dev.imprex.orebfuscator.config.migrations;

import org.jetbrains.annotations.NotNull;
import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;

class ConfigMigrationV4 implements ConfigMigration {

  @Override
  public int sourceVersion() {
    return 4;
  }

  @Override
  public @NotNull ConfigurationSection migrate(@NotNull ConfigurationSection root) {
    migrateWorlds(root.getSection("obfuscation"));
    migrateWorlds(root.getSection("proximity"));
    return root;
  }

  private static void migrateWorlds(ConfigurationSection configContainer) {
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
