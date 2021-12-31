package net.imprex.orebfuscator.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.NmsInstance;

public class OrebfuscatorProximityConfig extends AbstractWorldConfig implements ProximityConfig {

	private int distance = 8;
	private boolean useFastGazeCheck = false;

	private int defaultBlockFlags = (HideCondition.MATCH_ALL | BlockFlags.FLAG_USE_BLOCK_BELOW);

	private Map<Material, Integer> hiddenBlocks = new LinkedHashMap<>();

	OrebfuscatorProximityConfig(ConfigurationSection section) {
		super(section.getName());
		this.enabled = section.getBoolean("enabled", true);
		this.deserializeWorlds(section, "worlds");

		this.distance = section.getInt("distance", 8);
		if (this.distance < 1) {
			this.fail("distance must be higher than zero");
		}
		this.useFastGazeCheck = section.getBoolean("useFastGazeCheck", false);

		int defaultY = section.getInt("defaults.y", Short.MIN_VALUE);
		boolean defaultAbove = section.getBoolean("defaults.above", true);
		this.defaultBlockFlags = HideCondition.create(defaultY, defaultAbove);
		if (section.getBoolean("defaults.useBlockBelow", true)) {
			this.defaultBlockFlags |= BlockFlags.FLAG_USE_BLOCK_BELOW;
		}

		this.deserializeHiddenBlocks(section, "hiddenBlocks");
		this.deserializeRandomBlocks(section, "randomBlocks");
	}

	private void deserializeHiddenBlocks(ConfigurationSection section, String path) {
		ConfigurationSection blockSection = section.getConfigurationSection(path);
		if (blockSection == null) {
			return;
		}

		for (String blockName : blockSection.getKeys(false)) {
			Optional<Material> optional = NmsInstance.getMaterialByName(blockName);
			if (optional.isPresent()) {
				int blockFlags = this.defaultBlockFlags;

				// parse block specific height condition
				if (blockSection.isInt(blockName + ".y") && blockSection.isBoolean(blockName + ".above")) {
					blockFlags = HideCondition.remove(blockFlags);
					blockFlags |= HideCondition.create(blockSection.getInt(blockName + ".y"),
							blockSection.getBoolean(blockName + ".above"));
				}

				// parse block specific flags
				if (blockSection.isBoolean(blockName + ".useBlockBelow")) {
					if (blockSection.getBoolean(blockName + ".useBlockBelow")) {
						blockFlags |= BlockFlags.FLAG_USE_BLOCK_BELOW;
					} else {
						blockFlags &= ~BlockFlags.FLAG_USE_BLOCK_BELOW;
					}
				}

				this.hiddenBlocks.put(optional.get(), blockFlags);
			} else {
				warnUnkownBlock(section.getCurrentPath(), path, blockName);
			}
		}

		if (this.hiddenBlocks.isEmpty()) {
			this.failMissingOrEmpty(section, path);
		}
	}

	protected void serialize(ConfigurationSection section) {
		section.set("enabled", this.enabled);
		this.serializeWorlds(section, "worlds");
		section.set("distance", this.distance);
		section.set("useFastGazeCheck", this.useFastGazeCheck);

		section.set("defaults.y", HideCondition.getY(this.defaultBlockFlags));
		section.set("defaults.above", HideCondition.getAbove(this.defaultBlockFlags));
		section.set("defaults.useBlockBelow", BlockFlags.isUseBlockBelowBitSet(this.defaultBlockFlags));

		this.serializeHiddenBlocks(section, "hiddenBlocks");
		this.serializeRandomBlocks(section, "randomBlocks");
	}

	private void serializeHiddenBlocks(ConfigurationSection section, String path) {
		ConfigurationSection parentSection = section.createSection(path);

		for (Material material : this.hiddenBlocks.keySet()) {
			Optional<String> optional = NmsInstance.getNameByMaterial(material);
			if (optional.isPresent()) {
				ConfigurationSection childSection = parentSection.createSection(optional.get());

				int blockFlags = this.hiddenBlocks.get(material);
				if (!HideCondition.equals(blockFlags, this.defaultBlockFlags)) {
					childSection.set("y", HideCondition.getY(blockFlags));
					childSection.set("above", HideCondition.getAbove(blockFlags));
				}

				if (BlockFlags.isUseBlockBelowBitSet(blockFlags) != BlockFlags.isUseBlockBelowBitSet(this.defaultBlockFlags)) {
					childSection.set("useBlockBelow", BlockFlags.isUseBlockBelowBitSet(blockFlags));
				}
			} else {
				warnUnkownBlock(section.getCurrentPath(), path, material.name());
			}
		}
	}

	@Override
	public int distance() {
		return this.distance;
	}

	@Override
	public boolean useFastGazeCheck() {
		return this.useFastGazeCheck;
	}

	@Override
	public Iterable<Map.Entry<Material, Integer>> hiddenBlocks() {
		return this.hiddenBlocks.entrySet();
	}
}
