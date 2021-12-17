package net.imprex.orebfuscator.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.BlockPos;

public class OrebfuscatorProximityConfig extends AbstractWorldConfig implements ProximityConfig {

	private int distance = 8;
	private boolean useFastGazeCheck = false;

	private int defaultBlockFlags = (ProximityHeightCondition.MATCH_ALL | BlockFlags.FLAG_USE_BLOCK_BELOW);
	
	private Map<Material, Integer> hiddenBlocks = new LinkedHashMap<>();

	OrebfuscatorProximityConfig(ConfigurationSection section) {
		this.deserializeBase(section);
		this.deserializeWorlds(section, "worlds");

		if ((this.distance = section.getInt("distance", 8)) < 1) {
			this.fail("distance must be higher than zero");
		}
		this.useFastGazeCheck = section.getBoolean("useFastGazeCheck", false);
		
		this.defaultBlockFlags = ProximityHeightCondition.create(minY, maxY);
		if (section.getBoolean("useBlockBelow", true)) {
			this.defaultBlockFlags |= BlockFlags.FLAG_USE_BLOCK_BELOW;
		}

		this.deserializeHiddenBlocks(section, "hiddenBlocks");
		this.deserializeRandomBlocks(section, "randomBlocks");
	}

	protected void serialize(ConfigurationSection section) {
		this.serializeBase(section);
		this.serializeWorlds(section, "worlds");

		section.set("distance", this.distance);
		section.set("useFastGazeCheck", this.useFastGazeCheck);
		section.set("useBlockBelow", BlockFlags.isUseBlockBelowBitSet(this.defaultBlockFlags));

		this.serializeHiddenBlocks(section, "hiddenBlocks");
		this.serializeRandomBlocks(section, "randomBlocks");
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

				// LEGACY: parse pre 5.2.2 height condition
				if (blockSection.isInt(blockName + ".y") && blockSection.isBoolean(blockName + ".above")) {
					blockFlags = ProximityHeightCondition.remove(blockFlags);

					int y = blockSection.getInt(blockName + ".y");
					if (blockSection.getBoolean(blockName + ".above")) {
						blockFlags |= ProximityHeightCondition.create(y, BlockPos.MAX_Y);
					} else {
						blockFlags |= ProximityHeightCondition.create(BlockPos.MIN_Y, y);
					}
				}

				// parse block specific height condition
				if (blockSection.isInt(blockName + ".minY") && blockSection.isBoolean(blockName + ".maxY")) {
					blockFlags = ProximityHeightCondition.remove(blockFlags);
					blockFlags |= ProximityHeightCondition.create(
							blockSection.getInt(blockName + ".minY"),
							blockSection.getInt(blockName + ".maxY"));
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

	private void serializeHiddenBlocks(ConfigurationSection section, String path) {
		ConfigurationSection parentSection = section.createSection(path);

		for (Material material : this.hiddenBlocks.keySet()) {
			Optional<String> optional = NmsInstance.getNameByMaterial(material);
			if (optional.isPresent()) {
				ConfigurationSection childSection = parentSection.createSection(optional.get());

				int blockFlags = this.hiddenBlocks.get(material);
				if (!ProximityHeightCondition.equals(blockFlags, this.defaultBlockFlags)) {
					childSection.set("minY", ProximityHeightCondition.getMinY(blockFlags));
					childSection.set("maxY", ProximityHeightCondition.getMaxY(blockFlags));
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
