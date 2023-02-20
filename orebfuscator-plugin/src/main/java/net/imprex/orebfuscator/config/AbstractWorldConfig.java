package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.config.components.WeightedBlockList;
import net.imprex.orebfuscator.config.components.WorldMatcher;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.HeightAccessor;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.WeightedIntRandom;

public abstract class AbstractWorldConfig implements WorldConfig, ConfigParsingContext {

	private final String name;

	protected boolean enabled = false;
	protected int minY = BlockPos.MIN_Y;
	protected int maxY = BlockPos.MAX_Y;

	protected final List<WorldMatcher> worldMatchers = new ArrayList<>();

	private final List<WeightedBlockList> weightedBlockLists = new ArrayList<>();

	public AbstractWorldConfig(String name) {
		this.name = name;
	}

	public final void fail(String message) {
		this.enabled = false;
		OFCLogger.warn(message);
	}

	protected void deserializeBase(ConfigurationSection section) {
		this.enabled = section.getBoolean("enabled", true);
		this.minY = Math.max(BlockPos.MIN_Y, section.getInt("minY", BlockPos.MIN_Y));
		this.maxY = Math.min(BlockPos.MAX_Y, section.getInt("maxY", BlockPos.MAX_Y));
	}

	protected void serializeBase(ConfigurationSection section) {
		section.set("enabled", this.enabled);
		section.set("minY", this.minY);
		section.set("maxY", this.maxY);
	}

	protected void deserializeWorlds(ConfigurationSection section, String path) {
		section.getStringList(path).stream().map(WorldMatcher::parseMatcher).forEach(worldMatchers::add);

		if (this.worldMatchers.isEmpty()) {
			this.failMissingOrEmpty(section, path);
		}
	}

	protected void serializeWorlds(ConfigurationSection section, String path) {
		section.set(path, worldMatchers.stream().map(WorldMatcher::serialize).collect(Collectors.toList()));
	}

	protected void deserializeRandomBlocks(ConfigurationSection section, String path) {
		ConfigurationSection subSectionContainer = section.getConfigurationSection(path);
		if (subSectionContainer == null) {
			failMissingOrEmpty(section, path);
			return;
		}

		for (String subSectionName : subSectionContainer.getKeys(false)) {
			ConfigurationSection subSection = subSectionContainer.getConfigurationSection(subSectionName);
			this.weightedBlockLists.add(new WeightedBlockList(this, subSection));
		}

		if (this.weightedBlockLists.isEmpty()) {
			failMissingOrEmpty(section, path);
		}
	}

	protected void serializeRandomBlocks(ConfigurationSection section, String path) {
		ConfigurationSection subSectionContainer = section.createSection(path);

		for (WeightedBlockList weightedBlockList : this.weightedBlockLists) {
			weightedBlockList.serialize(subSectionContainer);
		}
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public int getMinY() {
		return this.minY;
	}

	@Override
	public int getMaxY() {
		return this.maxY;
	}

	@Override
	public boolean matchesWorldName(String worldName) {
		for (WorldMatcher matcher : this.worldMatchers) {
			if (matcher.test(worldName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean shouldObfuscate(int y) {
		return y >= this.minY && y <= this.maxY;
	}


	WeightedIntRandom[] createWeightedRandoms(HeightAccessor heightAccessor) {
		return WeightedBlockList.create(heightAccessor, this.weightedBlockLists);
	}
}
