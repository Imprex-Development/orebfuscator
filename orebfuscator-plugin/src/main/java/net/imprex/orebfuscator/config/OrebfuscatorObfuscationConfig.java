package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.BlockProperties;

public class OrebfuscatorObfuscationConfig extends AbstractWorldConfig implements ObfuscationConfig {

	private final Set<BlockProperties> hiddenBlocks = new LinkedHashSet<>();

	OrebfuscatorObfuscationConfig(ConfigurationSection section) {
		super(section.getName());
		this.deserializeBase(section);
		this.deserializeWorlds(section, "worlds");
		this.deserializeHiddenBlocks(section, "hiddenBlocks");
		this.deserializeRandomBlocks(section, "randomBlocks");
	}

	void serialize(ConfigurationSection section) {
		this.serializeBase(section);
		this.serializeWorlds(section, "worlds");
		this.serializeHiddenBlocks(section, "hiddenBlocks");
		this.serializeRandomBlocks(section, "randomBlocks");
	}

	private void deserializeHiddenBlocks(ConfigurationSection section, String path) {
		for (String blockName : section.getStringList(path)) {
			BlockProperties blockProperties = NmsInstance.getBlockByName(blockName);
			if (blockProperties != null) {
				this.hiddenBlocks.add(blockProperties);
			} else {
				warnUnknownBlock(section, path, blockName);
			}
		}

		if (this.hiddenBlocks.isEmpty()) {
			this.failMissingOrEmpty(section, path);
		}
	}

	private void serializeHiddenBlocks(ConfigurationSection section, String path) {
		List<String> blockNames = new ArrayList<>();

		for (BlockProperties block : this.hiddenBlocks) {
			blockNames.add(block.getName());
		}

		section.set(path, blockNames);
	}

	@Override
	public Iterable<BlockProperties> hiddenBlocks() {
		return this.hiddenBlocks;
	}
}
