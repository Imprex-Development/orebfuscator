package net.imprex.orebfuscator.config;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.imprex.orebfuscator.config.yaml.ConfigurationSection;
import org.joml.Matrix4f;

import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.config.components.WeightedBlockList;
import net.imprex.orebfuscator.config.context.ConfigParsingContext;
import net.imprex.orebfuscator.util.BlockProperties;

public class OrebfuscatorProximityConfig extends AbstractWorldConfig implements ProximityConfig {

	private int distance = 24;

	private boolean frustumCullingEnabled = true;
	private float frustumCullingMinDistance = 3;
	private float frustumCullingFov = 80f;

	private float frustumCullingMinDistanceSquared = 9;
	private final Matrix4f frustumCullingProjectionMatrix;

	private boolean rayCastCheckEnabled = false;
	private boolean rayCastCheckOnlyCheckCenter = false;
	private int defaultBlockFlags = (ProximityHeightCondition.MATCH_ALL | BlockFlags.FLAG_USE_BLOCK_BELOW);
	
	private boolean usesBlockSpecificConfigs = false;
	private final Map<BlockProperties, Integer> hiddenBlocks = new LinkedHashMap<>();
	private final Set<BlockProperties> allowForUseBlockBelow = new HashSet<>();

	OrebfuscatorProximityConfig(ConfigurationSection section, ConfigParsingContext context) {
		super(section.getName());
		this.deserializeBase(section);
		this.deserializeWorlds(section, context, "worlds");

		this.distance = section.getInt("distance", 24);
		context.errorMinValue("distance", 1, this.distance);

		this.frustumCullingEnabled = section.getBoolean("frustumCulling.enabled", false);
		this.frustumCullingMinDistance = section.getDouble("frustumCulling.minDistance", 3d).floatValue();
		this.frustumCullingFov = section.getDouble("frustumCulling.fov", 80d).floatValue();

		if (this.frustumCullingEnabled) {
			context.errorMinMaxValue("frustumCulling.fov", 10, 170, (int) this.frustumCullingFov);
		}

		this.frustumCullingMinDistanceSquared = this.frustumCullingMinDistance * this.frustumCullingMinDistance;
		this.frustumCullingProjectionMatrix = new Matrix4f() // create projection matrix with aspect 16:9
				.perspective(this.frustumCullingFov, 16f / 9f, 0.01f, 2 * this.distance);

		this.rayCastCheckEnabled = section.getBoolean("rayCastCheck.enabled", false);
		this.rayCastCheckOnlyCheckCenter = section.getBoolean("rayCastCheck.onlyCheckCenter", false);

		this.defaultBlockFlags = ProximityHeightCondition.create(this.minY, this.maxY);
		if (section.getBoolean("useBlockBelow", true)) {
			this.defaultBlockFlags |= BlockFlags.FLAG_USE_BLOCK_BELOW;
		}

		this.deserializeHiddenBlocks(section, context, "hiddenBlocks");
		this.deserializeRandomBlocks(section, context, "randomBlocks");

		for (WeightedBlockList blockList : this.weightedBlockLists) {
			this.allowForUseBlockBelow.addAll(blockList.getBlocks());
		}

		this.disableOnError(context);
	}

	protected void serialize(ConfigurationSection section) {
		this.serializeBase(section);
		this.serializeWorlds(section, "worlds");

		section.set("distance", this.distance);

		section.set("frustumCulling.enabled", this.frustumCullingEnabled);
		section.set("frustumCulling.minDistance", this.frustumCullingMinDistance);
		section.set("frustumCulling.fov", this.frustumCullingFov);

		section.set("rayCastCheck.enabled", this.rayCastCheckEnabled);
		section.set("rayCastCheck.onlyCheckCenter", this.rayCastCheckOnlyCheckCenter);
		section.set("useBlockBelow", BlockFlags.isUseBlockBelowBitSet(this.defaultBlockFlags));

		this.serializeHiddenBlocks(section, "hiddenBlocks");
		this.serializeRandomBlocks(section, "randomBlocks");
	}

	private void deserializeHiddenBlocks(ConfigurationSection section, ConfigParsingContext context, String path) {
		context = context.section(path);

		ConfigurationSection blockSection = section.getSection(path);
		if (blockSection == null) {
			return;
		}

		for (ConfigurationSection block : blockSection.getSubSections()) {
			BlockProperties blockProperties = OrebfuscatorNms.getBlockByName(block.getName());
			if (blockProperties == null) {
				context.warnUnknownBlock(block.getName());
			} else if (blockProperties.getDefaultBlockState().isAir()) {
				context.warnAirBlock(block.getName());
			} else {
				int blockFlags = this.defaultBlockFlags;

				// parse block specific height condition
				if (block.isInt("minY") && block.isInt("maxY")) {
					int minY = block.getInt("minY", this.minY);
					int maxY = block.getInt("maxY", this.maxY);

					blockFlags = ProximityHeightCondition.remove(blockFlags);
					blockFlags |= ProximityHeightCondition.create(
							Math.min(minY, maxY),
							Math.max(minY, maxY));
					usesBlockSpecificConfigs = true;
				}

				// parse block specific flags
				if (block.isBoolean("useBlockBelow")) {
					if (block.getBoolean("useBlockBelow", true)) {
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
			context.errorMissingOrEmpty();
		}
	}

	private void serializeHiddenBlocks(ConfigurationSection section, String path) {
		ConfigurationSection blockSection = section.createSection(path);

		for (Map.Entry<BlockProperties, Integer> entry : this.hiddenBlocks.entrySet()) {
			ConfigurationSection block = blockSection.createSection(entry.getKey().getKey().toString());

			int blockFlags = entry.getValue();
			if (!ProximityHeightCondition.equals(blockFlags, this.defaultBlockFlags)) {
				block.set("minY", ProximityHeightCondition.getMinY(blockFlags));
				block.set("maxY", ProximityHeightCondition.getMaxY(blockFlags));
			}

			if (BlockFlags.isUseBlockBelowBitSet(blockFlags) != BlockFlags.isUseBlockBelowBitSet(this.defaultBlockFlags)) {
				block.set("useBlockBelow", BlockFlags.isUseBlockBelowBitSet(blockFlags));
			}
		}
	}

	@Override
	public int distance() {
		return this.distance;
	}

	@Override
	public boolean frustumCullingEnabled() {
		return this.frustumCullingEnabled;
	}

	@Override
	public float frustumCullingMinDistanceSquared() {
		return this.frustumCullingMinDistanceSquared;
	}

	@Override
	public Matrix4f frustumCullingProjectionMatrix() {
		return new Matrix4f(frustumCullingProjectionMatrix);
	}

	@Override
	public boolean rayCastCheckEnabled() {
		return this.rayCastCheckEnabled;
	}

	@Override
	public boolean rayCastCheckOnlyCheckCenter() {
		return this.rayCastCheckOnlyCheckCenter;
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
