package dev.imprex.orebfuscator.config.migrations;

import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.util.BlockPos;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class ConfigMigrationV2 implements ConfigMigration {

  @Override
  public int sourceVersion() {
    return 2;
  }

  @Override
  public ConfigurationSection migrate(ConfigurationSection root) {
    convertRandomBlocksToSections(root.getSection("obfuscation"));
    convertRandomBlocksToSections(root.getSection("proximity"));
    return root;
  }

  private static void convertRandomBlocksToSections(@Nullable ConfigurationSection configContainer) {
    if (configContainer == null) {
      return;
    }

    for (ConfigurationSection config : configContainer.getSubSections()) {
      ConfigurationSection blockSection = config.getSection("randomBlocks");
      if (blockSection == null) {
        continue;
      }

      ConfigurationSection newBlockSection = config.createSection("randomBlocks");
      newBlockSection = newBlockSection.createSection("section-global");
      newBlockSection.set("minY", BlockPos.MIN_Y);
      newBlockSection.set("maxY", BlockPos.MAX_Y);
      newBlockSection = newBlockSection.createSection("blocks");

      for (String blockName : blockSection.getKeys()) {
        newBlockSection.set(blockName, blockSection.getInt(blockName));
      }
    }
  }
}
