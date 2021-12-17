package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.NmsInstance;

public class OrebfuscatorObfuscationConfig extends AbstractWorldConfig implements ObfuscationConfig {

	private final Set<Material> hiddenBlocks = new LinkedHashSet<>();

	OrebfuscatorObfuscationConfig(ConfigurationSection section) {
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
			Optional<Material> optional = NmsInstance.getMaterialByName(blockName);
			if (optional.isPresent()) {
				this.hiddenBlocks.add(optional.get());
			} else {
				warnUnkownBlock(section.getCurrentPath(), path, blockName);
			}
		}

		if (this.hiddenBlocks.isEmpty()) {
			this.failMissingOrEmpty(section, path);
		}
	}

	private void serializeHiddenBlocks(ConfigurationSection section, String path) {
		List<String> blockNames = new ArrayList<>();

		for (Material material : this.hiddenBlocks) {
			Optional<String> optional = NmsInstance.getNameByMaterial(material);
			if (optional.isPresent()) {
				blockNames.add(optional.get());
			} else {
				warnUnkownBlock(section.getCurrentPath(), path, material.name());
			}
		}

		section.set(path, blockNames);
	}

	@Override
	public Iterable<Material> hiddenBlocks() {
		return this.hiddenBlocks;
	}
}
