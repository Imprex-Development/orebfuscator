package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.config.context.ConfigParsingContext;
import net.imprex.orebfuscator.util.BlockProperties;

public class OrebfuscatorObfuscationConfig extends AbstractWorldConfig implements ObfuscationConfig {

	private boolean layerObfuscation = false;

	private final Set<BlockProperties> hiddenBlocks = new LinkedHashSet<>();

	OrebfuscatorObfuscationConfig(ConfigurationSection section, ConfigParsingContext context) {
		super(section.getName());
		this.deserializeBase(section);
		this.deserializeWorlds(section, context, "worlds");
		this.layerObfuscation = section.getBoolean("layerObfuscation", false);
		this.deserializeHiddenBlocks(section, context, "hiddenBlocks");
		this.deserializeRandomBlocks(section, context, "randomBlocks");
		this.disableOnError(context);
	}

	void serialize(ConfigurationSection section) {
		this.serializeBase(section);
		this.serializeWorlds(section, "worlds");
		section.set("layerObfuscation", this.layerObfuscation);
		this.serializeHiddenBlocks(section, "hiddenBlocks");
		this.serializeRandomBlocks(section, "randomBlocks");
	}

	private void deserializeHiddenBlocks(ConfigurationSection section, ConfigParsingContext context, String path) {
		context = context.section(path);

		for (String blockName : section.getStringList(path)) {
			BlockProperties blockProperties = OrebfuscatorNms.getBlockByName(blockName);
			if (blockProperties == null) {
				context.warnUnknownBlock(blockName);
			} else if (blockProperties.getDefaultBlockState().isAir()) {
				context.warnAirBlock(blockName);
			} else {
				this.hiddenBlocks.add(blockProperties);
			}
		}

		if (this.hiddenBlocks.isEmpty()) {
			context.errorMissingOrEmpty();
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
	public boolean layerObfuscation() {
		return this.layerObfuscation;
	}

	@Override
	public Iterable<BlockProperties> hiddenBlocks() {
		return this.hiddenBlocks;
	}
}
