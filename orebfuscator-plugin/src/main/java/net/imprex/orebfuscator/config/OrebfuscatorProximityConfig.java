package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.WeightedRandom;

public class OrebfuscatorProximityConfig implements ProximityConfig {

	private final List<String> worlds = new ArrayList<>();

	private boolean enabled;
	private int distance;
	private int distanceSquared;
	private boolean useFastGazeCheck;

	private short defaultHideCondition = (short) (HideCondition.EMPTY | BlockMask.FLAG_USE_BLOCK_BELOW);

	private Map<Material, Short> hiddenBlocks = new LinkedHashMap<>();

	private Map<Material, Integer> randomBlocks = new LinkedHashMap<>();
	private final List<Integer> randomBlockIds = new ArrayList<>();
	private WeightedRandom<Integer> randomMaterials = new WeightedRandom<>();

	@Override
	public void initialize() {
		this.randomMaterials.clear();
		for (Entry<Material, Integer> entry : this.randomBlocks.entrySet()) {
			int blockId = NmsInstance.getMaterialIds(entry.getKey()).iterator().next();
			this.randomMaterials.add(entry.getValue(), blockId);
			this.randomBlockIds.add(blockId);
		}
	}

	protected void serialize(ConfigurationSection section) {
		this.enabled(section.getBoolean("enabled", true));

		List<String> worldNameList = section.getStringList("worlds");
		if (worldNameList == null || worldNameList.isEmpty()) {
			this.failSerialize(
					String.format("config section '%s.worlds' is missing or empty", section.getCurrentPath()));
			return;
		}
		this.worlds.clear();
		this.worlds.addAll(worldNameList);

		this.distance(section.getInt("distance", 8));
		this.useFastGazeCheck(section.getBoolean("useFastGazeCheck", true));

		int y = section.getInt("defaults.y", 0);
		boolean above = section.getBoolean("defaults.above", true);
		this.defaultHideCondition = HideCondition.create(y, above);
		if (section.getBoolean("defaults.useBlockBelow", true)) {
			this.defaultHideCondition |= BlockMask.FLAG_USE_BLOCK_BELOW;
		}

		this.serializeHiddenBlocks(section);
		if (this.hiddenBlocks.isEmpty()) {
			this.enabled = false;
			this.failSerialize(
					String.format("config section '%s.hiddenBlocks' is missing or empty", section.getCurrentPath()));
			return;
		}

		ConfigParser.serializeRandomMaterialList(section, this.randomBlocks, "randomBlocks");
		if (this.randomBlocks.isEmpty()) {
			this.enabled = false;
			this.failSerialize(
					String.format("config section '%s.randomBlocks' is missing or empty", section.getCurrentPath()));
		}
	}

	protected void deserialize(ConfigurationSection section) {
		section.set("enabled", this.enabled);
		section.set("worlds", this.worlds);
		section.set("distance", this.distance);
		section.set("useFastGazeCheck", this.useFastGazeCheck);

		section.set("defaults.y", HideCondition.getY(this.defaultHideCondition));
		section.set("defaults.above", HideCondition.getAbove(this.defaultHideCondition));
		section.set("defaults.useBlockBelow", BlockMask.isUseBlockBelowSet(this.defaultHideCondition));
		
		this.deserializeHiddenBlocks(section, this.hiddenBlocks, "hiddenBlocks");
		ConfigParser.deserializeRandomMaterialList(section, randomBlocks, "randomBlocks");
	}

	private void serializeHiddenBlocks(ConfigurationSection section) {
		this.hiddenBlocks.clear();

		ConfigurationSection materialSection = section.getConfigurationSection("hiddenBlocks");
		if (materialSection == null) {
			return;
		}

		for (String name : materialSection.getKeys(false)) {
			Optional<Material> material = NmsInstance.getMaterialByName(name);

			if (!material.isPresent()) {
				OFCLogger.warn(String.format("config section '%s.hiddenBlocks' contains unknown block '%s'",
						section.getCurrentPath(), name));
				continue;
			}

			short hideCondition = this.defaultHideCondition;
			if (materialSection.isInt(name + ".y") && materialSection.isBoolean(name + ".above")) {
				hideCondition = HideCondition.create(materialSection.getInt(name + ".y"),
						materialSection.getBoolean(name + ".above"));
			}

			if (materialSection.isBoolean(name + ".useBlockBelow")) {
				if (materialSection.getBoolean(name + ".useBlockBelow")) {
					hideCondition |= BlockMask.FLAG_USE_BLOCK_BELOW;
				} else {
					hideCondition &= ~BlockMask.FLAG_USE_BLOCK_BELOW;
				}
			}

			this.hiddenBlocks.put(material.get(), hideCondition);
		}
	}

	private void deserializeHiddenBlocks(ConfigurationSection section, Map<Material, Short> hiddenBlocks, String path) {
		ConfigurationSection materialSection = section.createSection(path);
		for (Entry<Material, Short> entry : this.hiddenBlocks.entrySet()) {
			Material material = entry.getKey();
			Optional<String> optional = NmsInstance.getNameByMaterial(material);
			if (!optional.isPresent()) {
				OFCLogger.warn(String.format("config section '%s.%s' contains unknown block name '%s'",
						section.getCurrentPath(), path, material != null ? material.name() : null));
				continue;
			}

			String name = optional.get();
			short condition = entry.getValue();
			if (condition == HideCondition.EMPTY) {
				materialSection.createSection(name);
			} else {
				materialSection.set(name + ".y", HideCondition.getY(condition));
				materialSection.set(name + ".above", HideCondition.getAbove(condition));
			}

			if (BlockMask.isUseBlockBelowSet(condition) != BlockMask.isUseBlockBelowSet(this.defaultHideCondition)) {
				materialSection.set(name + ".useBlockBelow", BlockMask.isUseBlockBelowSet(condition));
			}
		}
	}

	private void failSerialize(String message) {
		this.enabled = false;
		OFCLogger.warn(message);
	}

	@Override
	public boolean enabled() {
		return this.enabled;
	}

	@Override
	public void enabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public List<String> worlds() {
		return this.worlds;
	}

	@Override
	public int distance() {
		return this.distance;
	}

	@Override
	public void distance(int distance) {
		if (distance < 1) {
			throw new IllegalArgumentException("distance must higher than zero");
		}
		this.distance = distance;
		this.distanceSquared = this.distance * this.distance;
	}

	@Override
	public int distanceSquared() {
		return this.distanceSquared;
	}

	@Override
	public boolean useFastGazeCheck() {
		return this.useFastGazeCheck;
	}

	@Override
	public void useFastGazeCheck(boolean fastGaze) {
		this.useFastGazeCheck = fastGaze;
	}

	@Override
	public Map<Material, Short> hiddenBlocks() {
		return this.hiddenBlocks;
	}

	@Override
	public Map<Material, Integer> randomBlocks() {
		return this.randomBlocks;
	}

	@Override
	public int randomBlockId() {
		return this.randomMaterials.next();
	}
}
