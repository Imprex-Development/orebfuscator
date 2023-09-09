package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.OFCLogger;

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
			BlockProperties blockProperties = OrebfuscatorNms.getBlockByName(blockName);
			if (blockProperties == null) {
				warnUnknownBlock(section, path, blockName);
			} else if (blockProperties.getDefaultBlockState().isAir()) {
		        OFCLogger.warn(String.format("config section '%s.%s' contains air block '%s', skipping",
		                section.getCurrentPath(), path, blockName));
			} else {
				this.hiddenBlocks.add(blockProperties);
			}
		}

		if (this.hiddenBlocks.isEmpty()) {
			this.failMissingOrEmpty(section, path);
		}
	}

	private void serializeHiddenBlocks(ConfigurationSection section, String path) {
		List<String> blockNames = new ArrayList<>();

		for (BlockProperties block : this.hiddenBlocks) {
			blockNames.add(block.getKey().toString());
		}

		section.set(path, blockNames);
	}

	@Override
	public Iterable<BlockProperties> hiddenBlocks() {
		return this.hiddenBlocks;
	}
}
