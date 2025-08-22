package dev.imprex.orebfuscator.config.migrations;

import org.bukkit.configuration.ConfigurationSection;

import dev.imprex.orebfuscator.util.BlockPos;

class ConfigMigrationV2 implements ConfigMigration {

	@Override
	public int sourceVersion() {
		return 2;
	}

	@Override
	public ConfigurationSection migrate(ConfigurationSection section) {
		this.convertRandomBlocksToSections(section.getConfigurationSection("obfuscation"));
		this.convertRandomBlocksToSections(section.getConfigurationSection("proximity"));
		return section;
	}


	private void convertRandomBlocksToSections(ConfigurationSection parentSection) {
		for (String key : parentSection.getKeys(false)) {
			ConfigurationSection config = parentSection.getConfigurationSection(key);
			ConfigurationSection blockSection = config.getConfigurationSection("randomBlocks");
			if (blockSection == null) {
				continue;
			}

			ConfigurationSection newBlockSection = config.createSection("randomBlocks");
			newBlockSection = newBlockSection.createSection("section-global");
			newBlockSection.set("minY", BlockPos.MIN_Y);
			newBlockSection.set("maxY", BlockPos.MAX_Y);
			newBlockSection = newBlockSection.createSection("blocks");

			for (String blockName : blockSection.getKeys(false)) {
				newBlockSection.set(blockName, blockSection.getInt(blockName, 1));
			}
		}
	}
}
