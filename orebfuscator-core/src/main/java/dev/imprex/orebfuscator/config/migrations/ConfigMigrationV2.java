package dev.imprex.orebfuscator.config.migrations;

import org.jetbrains.annotations.NotNull;
import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.util.BlockPos;

class ConfigMigrationV2 implements ConfigMigration {

  @Override
  public int sourceVersion() {
    return 2;
  }

  @Override
  public @NotNull ConfigurationSection migrate(@NotNull ConfigurationSection root) {
    convertRandomBlocksToSections(root.getSection("obfuscation"));
    convertRandomBlocksToSections(root.getSection("proximity"));
    return root;
  }

  private static void convertRandomBlocksToSections(ConfigurationSection configContainer) {
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
