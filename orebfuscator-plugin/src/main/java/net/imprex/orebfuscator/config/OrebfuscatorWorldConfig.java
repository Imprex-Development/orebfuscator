package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import com.lishid.orebfuscator.Orebfuscator;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.WeightedRandom;

public class OrebfuscatorWorldConfig implements WorldConfig {

	private boolean enabled;
	private List<World> worlds = new ArrayList<>();
	private Set<Material> darknessBlocks = new HashSet<>();
	private Set<Material> hiddenBlocks = new HashSet<>();

	private Map<Material, Integer> randomBlocks = new HashMap<>();
	private WeightedRandom<Set<Integer>> randomMaterials = new WeightedRandom<>();

	protected void initialize() {
		this.randomMaterials.clear();
		for (Entry<Material, Integer> entry : this.randomBlocks.entrySet()) {
			this.randomMaterials.add(entry.getValue(), NmsInstance.get().getMaterialIds(entry.getKey()));
		}
	}

	protected void serialize(ConfigurationSection section) {
		this.enabled = section.getBoolean("enabled", true);

		this.serializeWorldList(section, this.worlds, "worlds");
		if (this.worlds.isEmpty()) {
			this.failSerialize(
					String.format("config section '%s.worlds' is missing or empty", section.getCurrentPath()));
			return;
		}

		this.serializeMaterialSet(section, this.darknessBlocks, "darknessBlocks");
		this.serializeMaterialSet(section, this.hiddenBlocks, "hiddenBlocks");
		if (this.darknessBlocks.isEmpty() && this.hiddenBlocks.isEmpty()) {
			this.failSerialize(String.format("config section '%s' is missing 'darknessBlocks' and 'hiddenBlocks'",
					section.getCurrentPath()));
			return;
		}

		ConfigParser.serializeRandomMaterialList(section, this.randomBlocks, "randomBlocks");
		if (this.randomBlocks.isEmpty()) {
			this.failSerialize(
					String.format("config section '%s.randomBlocks' is missing or empty", section.getCurrentPath()));
		}
	}

	public void serializeWorldList(ConfigurationSection section, List<World> worlds, String path) {
		worlds.clear();

		List<String> worldNameList = section.getStringList(path);
		if (worldNameList == null || worldNameList.isEmpty()) {
			return;
		}

		for (String worldName : worldNameList) {
			World world = Bukkit.getWorld(worldName);

			if (world == null) {
				Orebfuscator.LOGGER.warning(String.format("config section '%s.%s' contains unknown world '%s'",
						section.getCurrentPath(), path, worldName));
				continue;
			}

			worlds.add(world);
		}
	}

	public void serializeMaterialSet(ConfigurationSection section, Set<Material> materials, String path) {
		materials.clear();

		List<String> materialNameList = section.getStringList(path);
		if (materialNameList == null || materialNameList.isEmpty()) {
			return;
		}

		for (String materialName : materialNameList) {
			Material material = Material.matchMaterial(materialName);

			if (material == null) {
				Orebfuscator.LOGGER.warning(String.format("config section '%s.%s' contains unknown block '%s'",
						section.getCurrentPath(), path, materialName));
				continue;
			}

			materials.add(material);
		}
	}

	private void failSerialize(String message) {
		this.enabled = false;
		Orebfuscator.LOGGER.warning(message);
	}

	@Override
	public Set<Integer> randomBlockId() {
		return this.randomMaterials.next();
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
	public Set<Material> darknessBlocks() {
		return Collections.unmodifiableSet(this.darknessBlocks);
	}

	@Override
	public Set<Material> hiddenBlocks() {
		return Collections.unmodifiableSet(this.hiddenBlocks);
	}
}
