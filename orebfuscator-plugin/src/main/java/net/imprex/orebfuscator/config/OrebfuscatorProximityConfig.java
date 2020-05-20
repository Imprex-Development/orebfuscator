package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.WeightedRandom;

public class OrebfuscatorProximityConfig implements ProximityConfig {

	private static final short EMPTY_HIDE_CONDITION = createHideCondition(0, true);

	private static short createHideCondition(int y, boolean above) {
		return (short) ((y & 0xFF) << 8 | (above ? 0x80 : 0x00));
	}

	static boolean matchHideCondition(short hideCondition, int y) {
		int expectedY = hideCondition >> 8;
		if ((hideCondition & 0x80) != 0) {
			return expectedY < y;
		} else {
			return expectedY > y;
		}
	}

	private final List<World> worlds = new ArrayList<>();

	private boolean enabled;
	private int distance;
	private int distanceSquared;
	private boolean useFastGazeCheck;

	private Map<Material, Short> hiddenBlocks = new HashMap<>();

	private Map<Material, Integer> randomBlocks = new HashMap<>();
	private final List<Integer> randomBlockIds = new ArrayList<>();
	private WeightedRandom<Integer> randomMaterials = new WeightedRandom<>();

	protected void initialize() {
		this.randomMaterials.clear();
		for (Entry<Material, Integer> entry : this.randomBlocks.entrySet()) {
			int blockId = NmsInstance.get().getMaterialIds(entry.getKey()).iterator().next();
			this.randomMaterials.add(entry.getValue(), blockId);
			this.randomBlockIds.add(blockId);
		}
	}

	protected void serialize(ConfigurationSection section) {
		this.enabled = section.getBoolean("enabled", true);

		ConfigParser.serializeWorldList(section, this.worlds, "worlds");
		if (this.worlds.isEmpty()) {
			this.failSerialize(
					String.format("config section '%s.worlds' is missing or empty", section.getCurrentPath()));
			return;
		}

		this.distance = section.getInt("distance", 8);
		this.distanceSquared = this.distance * this.distance;
		this.useFastGazeCheck = section.getBoolean("useFastGazeCheck", true);

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

	private void serializeHiddenBlocks(ConfigurationSection section) {
		this.hiddenBlocks.clear();

		ConfigurationSection materialSection = section.getConfigurationSection("hiddenBlocks");
		if (materialSection == null) {
			return;
		}

		Set<String> materialNames = materialSection.getKeys(false);
		if (materialNames.isEmpty()) {
			return;
		}

		for (String materialName : materialNames) {
			Material material = Material.matchMaterial(materialName);

			if (material == null) {
				OFCLogger.warn(String.format("config section '%s.hiddenBlocks' contains unknown block '%s'",
						section.getCurrentPath(), materialName));
				continue;
			}

			short hideCondition = EMPTY_HIDE_CONDITION;
			if (materialSection.isInt(materialName + ".y") && materialSection.isBoolean(materialName + ".above")) {
				hideCondition = createHideCondition(materialSection.getInt(materialName + ".y"), materialSection.getBoolean(materialName + ".above"));
			}

			this.hiddenBlocks.put(material, hideCondition);
		}
	}

	private void failSerialize(String message) {
		this.enabled = false;
		OFCLogger.warn(message);
	}

	public Set<Map.Entry<Material, Short>> getHiddenBlocks() {
		return hiddenBlocks.entrySet();
	}

	@Override
	public boolean enabled() {
		return this.enabled;
	}

	@Override
	public List<World> worlds() {
		return Collections.unmodifiableList(this.worlds);
	}

	@Override
	public int distance() {
		return this.distance;
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
	public List<Integer> randomBlocks() {
		return this.randomBlockIds;
	}

	@Override
	public int randomBlockId() {
		return this.randomMaterials.next();
	}
}
