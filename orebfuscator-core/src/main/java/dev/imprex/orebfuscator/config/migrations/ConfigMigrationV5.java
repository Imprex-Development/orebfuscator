package dev.imprex.orebfuscator.config.migrations;

import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ConfigMigrationV5 implements ConfigMigration {

  @Override
  public int sourceVersion() {
    return 5;
  }

  @Override
  public ConfigurationSection migrate(ConfigurationSection root) {
    ConfigMigration.migrateNames(root.getSection("advanced"), List.of(
        Map.entry("obfuscation.threads", "threads")
    ));
    return root;
  }
}
