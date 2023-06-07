package net.imprex.orebfuscator.config;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.config.components.WeightedBlockList;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorProximityConfig extends AbstractWorldConfig implements ProximityConfig {

	private int distance = 8;
	private boolean useFastGazeCheck = false;

	private int defaultBlockFlags = (ProximityHeightCondition.MATCH_ALL | BlockFlags.FLAG_USE_BLOCK_BELOW);
	
	private boolean usesBlockSpecificConfigs = false;
	private Map<BlockProperties, Integer> hiddenBlocks = new LinkedHashMap<>();
	private Set<BlockProperties> allowForUseBlockBelow = new HashSet<>();

	OrebfuscatorProximityConfig(ConfigurationSection section) {
		super(section.getName());
		this.deserializeBase(section);
		this.deserializeWorlds(section, "worlds");

		// LEGACY: transform to post 5.2.2
		if (section.isConfigurationSection("defaults")) {
			int y = section.getInt("defaults.y");
			if (section.getBoolean("defaults.above")) {
				this.minY = y;
				this.maxY = BlockPos.MAX_Y;
			} else {
				this.minY = BlockPos.MIN_Y;
				this.minY = y;
			}
			section.set("useBlockBelow", section.getBoolean("defaults.useBlockBelow"));
		}

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

		for (WeightedBlockList blockList : this.weightedBlockLists) {
			this.allowForUseBlockBelow.addAll(blockList.getBlocks());
		}
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
			BlockProperties blockProperties = NmsInstance.getBlockByName(blockName);
			if (blockProperties == null) {
				warnUnknownBlock(section, path, blockName);
			} else if (blockProperties.getDefaultBlockState().isAir()) {
		        OFCLogger.warn(String.format("config section '%s.%s' contains air block '%s', skipping",
		                section.getCurrentPath(), path, blockName));
			} else {
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

					usesBlockSpecificConfigs = true;
				}

				// parse block specific height condition
				if (blockSection.isInt(blockName + ".minY") && blockSection.isInt(blockName + ".maxY")) {
					int minY = blockSection.getInt(blockName + ".minY");
					int maxY = blockSection.getInt(blockName + ".maxY");

					blockFlags = ProximityHeightCondition.remove(blockFlags);
					blockFlags |= ProximityHeightCondition.create(
							Math.min(minY, maxY),
							Math.max(minY, maxY));
					usesBlockSpecificConfigs = true;
				}

				// parse block specific flags
				if (blockSection.isBoolean(blockName + ".useBlockBelow")) {
					if (blockSection.getBoolean(blockName + ".useBlockBelow")) {
						blockFlags |= BlockFlags.FLAG_USE_BLOCK_BELOW;
					} else {
						blockFlags &= ~BlockFlags.FLAG_USE_BLOCK_BELOW;
					}
					usesBlockSpecificConfigs = true;
				}

				this.hiddenBlocks.put(blockProperties, blockFlags);
			}
		}

		if (this.hiddenBlocks.isEmpty()) {
			this.failMissingOrEmpty(section, path);
		}
	}

	private void serializeHiddenBlocks(ConfigurationSection section, String path) {
		ConfigurationSection parentSection = section.createSection(path);

		for (Map.Entry<BlockProperties, Integer> entry : this.hiddenBlocks.entrySet()) {
			ConfigurationSection childSection = parentSection.createSection(entry.getKey().getKey().toString());

			int blockFlags = entry.getValue();
			if (!ProximityHeightCondition.equals(blockFlags, this.defaultBlockFlags)) {
				childSection.set("minY", ProximityHeightCondition.getMinY(blockFlags));
				childSection.set("maxY", ProximityHeightCondition.getMaxY(blockFlags));
			}

			if (BlockFlags.isUseBlockBelowBitSet(blockFlags) != BlockFlags.isUseBlockBelowBitSet(this.defaultBlockFlags)) {
				childSection.set("useBlockBelow", BlockFlags.isUseBlockBelowBitSet(blockFlags));
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
	public Iterable<Map.Entry<BlockProperties, Integer>> hiddenBlocks() {
		return this.hiddenBlocks.entrySet();
	}

	public Iterable<BlockProperties> allowForUseBlockBelow() {
		return this.allowForUseBlockBelow;
	}

	boolean usesBlockSpecificConfigs() {
		return usesBlockSpecificConfigs;
	}
}
