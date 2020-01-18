package net.imprex.orebfuscator.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import com.lishid.orebfuscator.Orebfuscator;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.WeightedRandom;

public class OrebfuscatorProximityHiderConfig implements ProximityHiderConfig {

	private boolean enabled;
	private int distance;
	private int distanceSquared;

	private Map<Material, ShouldHideConfig> hiddenBlocks = new HashMap<>();
	private Map<Integer, ShouldHideConfig> hiddenMaterials = new HashMap<>();

	private Map<Material, Integer> randomBlocks = new HashMap<>();
	private WeightedRandom<Set<Integer>> randomMaterials = new WeightedRandom<>();

	protected void initialize() {
		this.hiddenMaterials.clear();
		for (Entry<Material, ShouldHideConfig> entry : this.hiddenBlocks.entrySet()) {
			for (int id : NmsInstance.get().getMaterialIds(entry.getKey())) {
				this.hiddenMaterials.put(id, entry.getValue());
			}
		}

		this.randomMaterials.clear();
		for (Entry<Material, Integer> entry : this.randomBlocks.entrySet()) {
			this.randomMaterials.add(entry.getValue(), NmsInstance.get().getMaterialIds(entry.getKey()));
		}
	}

	protected void serialize(ConfigurationSection section) {
		this.enabled = section.getBoolean("enabled", true);
		this.distance = section.getInt("distance", 8);
		this.distanceSquared = this.distance * this.distance;

		this.serializeHiddenBlocks(section);
		if (this.randomBlocks.isEmpty()) {
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

		ShouldHideConfig emptyHideConfig = new ShouldHideConfig(0, true);
		for (String materialName : materialNames) {
			Material material = Material.matchMaterial(materialName);

			if (material == null) {
				Orebfuscator.LOGGER.warning(String.format("config section '%s.hiddenBlocks' contains unknown block '%s'",
						section.getCurrentPath(), materialName));
				continue;
			}
			ConfigurationSection configurationSection = section.getConfigurationSection(materialName);
			ShouldHideConfig hideConfig = emptyHideConfig;

			if (configurationSection != null) {
				if (configurationSection.isInt("y") && configurationSection.isBoolean("above")) {
					hideConfig = new ShouldHideConfig(configurationSection.getInt("y"), configurationSection.getBoolean("above"));
				}
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
	public int distance() {
		return this.distance;
	}

	@Override
	public int distanceSquared() {
		return this.distanceSquared;
	}

	@Override
	public Set<Integer> randomBlockId() {
		return Collections.unmodifiableSet(this.randomMaterials.next());
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

	private class ShouldHideConfig {

		private final int y;
		private final boolean higher;

		public ShouldHideConfig(int y, boolean higher) {
			this.y = y;
			this.higher = higher;
		}

		public boolean shouldHide(int y) {
			if (this.higher) {
				return this.y <= y;
			} else {
				return this.y >= y;
			}
		}
	}
}
