package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.WeightedRandom;

public abstract class AbstractWorldConfig implements WorldConfig {

	private final String name;

	protected boolean enabled = false;

	protected final List<WorldMatcher> worldMatchers = new ArrayList<>();

	protected final Map<Material, Integer> randomBlocks = new LinkedHashMap<>();
	protected final WeightedRandom<Integer> weightedBlockIds = new WeightedRandom<>();

	public AbstractWorldConfig(String name) {
		this.name = name;
	}

	protected static void warnUnkownBlock(String section, String path, String name) {
		OFCLogger.warn(String.format("config section '%s.%s' contains unknown block '%s'", section, path, name));
	}

	protected final void failMissingOrEmpty(ConfigurationSection section, String missingSection) {
		this.fail(String.format("config section '%s.%s' is missing or empty", section.getCurrentPath(), missingSection));
	}

	protected final void fail(String message) {
		this.enabled = false;
		OFCLogger.warn(message);
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
		ConfigurationSection blockSection = section.getConfigurationSection(path);
		if (blockSection == null) {
			return;
		}

		for (String blockName : blockSection.getKeys(false)) {
			Optional<Material> optional = NmsInstance.getMaterialByName(blockName);
			if (optional.isPresent()) {
				int weight = blockSection.getInt(blockName, 1);
				this.randomBlocks.put(optional.get(), weight);

				NmsInstance.getFirstBlockId(optional.get()).ifPresent(blockId -> {
					this.weightedBlockIds.add(weight, blockId);
				});
			} else {
				warnUnkownBlock(section.getCurrentPath(), path, blockName);
			}
		}

		if (this.randomBlocks.isEmpty()) {
			this.failMissingOrEmpty(section, path);
		}
	}

	protected void serializeRandomBlocks(ConfigurationSection section, String path) {
		ConfigurationSection blockSection = section.createSection(path);

		for (Material material : this.randomBlocks.keySet()) {
			Optional<String> optional = NmsInstance.getNameByMaterial(material);
			if (optional.isPresent()) {
				blockSection.set(optional.get(), this.randomBlocks.get(material));
			} else {
				warnUnkownBlock(section.getCurrentPath(), path, material.name());
			}
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
	public boolean matchesWorldName(String worldName) {
		for (WorldMatcher matcher : this.worldMatchers) {
			if (matcher.test(worldName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int nextRandomBlockId() {
		return this.weightedBlockIds.next();
	}
}
