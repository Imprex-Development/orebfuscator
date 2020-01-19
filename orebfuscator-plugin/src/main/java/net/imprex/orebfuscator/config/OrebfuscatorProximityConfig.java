package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import com.lishid.orebfuscator.Orebfuscator;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.WeightedRandom;

public class OrebfuscatorProximityConfig implements ProximityConfig {

	private static final ShouldHideConfig EMPTY_HIDE_CONFIG = new ShouldHideConfig(0, true);

	private final List<World> worlds = new ArrayList<>();

	private boolean enabled;
	private int distance;
	private int distanceSquared;
	private boolean useFastGazeCheck;

	private Map<Material, ShouldHideConfig> hiddenBlocks = new HashMap<>();
	private Map<Integer, ShouldHideConfig> hiddenMaterials = new HashMap<>();

	private Map<Material, Integer> randomBlocks = new HashMap<>();

	private List<Integer> randomBlockIds = new ArrayList<>();
	private WeightedRandom<Integer> randomMaterials = new WeightedRandom<>();

	protected void initialize() {
		this.hiddenMaterials.clear();
		for (Entry<Material, ShouldHideConfig> entry : this.hiddenBlocks.entrySet()) {
			for (int id : NmsInstance.get().getMaterialIds(entry.getKey())) {
				this.hiddenMaterials.put(id, entry.getValue());
			}
		}

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
				Orebfuscator.LOGGER.warning(String.format("config section '%s.hiddenBlocks' contains unknown block '%s'",
						section.getCurrentPath(), materialName));
				continue;
			}

			ShouldHideConfig hideConfig = EMPTY_HIDE_CONFIG;
			if (materialSection.isInt(materialName + ".y") && materialSection.isBoolean(materialName + ".above")) {
				hideConfig = new ShouldHideConfig(materialSection.getInt(materialName + ".y"), materialSection.getBoolean(materialName + ".above"));
			}

			this.hiddenBlocks.put(material, hideConfig);
		}
	}

	private void failSerialize(String message) {
		this.enabled = false;
		Orebfuscator.LOGGER.warning(message);
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
	public Collection<Integer> randomBlocks() {
		return this.randomBlockIds;
	}

	@Override
	public int randomBlockId() {
		return this.randomMaterials.next();
	}

	@Override
	public Set<Integer> hiddenBlocks() {
		return Collections.unmodifiableSet(this.hiddenMaterials.keySet());
	}

	@Override
	public boolean shouldHide(int y, int id) {
		ShouldHideConfig shouldHide = this.hiddenMaterials.get(id);

		if (shouldHide == null) {
			return false;
		}

		return shouldHide.shouldHide(y);
	}

	private static class ShouldHideConfig {

		private final int y;
		private final boolean higher;

		public ShouldHideConfig(int y, boolean higher) {
			this.y = y;
			this.higher = higher;
		}

		public boolean shouldHide(int y) {
			if (this.higher) {
				return this.y < y;
			} else {
				return this.y > y;
			}
		}
	}
}
