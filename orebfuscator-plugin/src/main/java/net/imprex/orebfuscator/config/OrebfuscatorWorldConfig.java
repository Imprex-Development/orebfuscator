package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import com.lishid.orebfuscator.Orebfuscator;

import net.imprex.orebfuscator.util.WeightedRandom;

public class OrebfuscatorWorldConfig implements WorldConfig {

	private boolean enabled;
	private List<World> worlds = new ArrayList<>();
	private Set<Material> darknessMaterials = new HashSet<>();
	private Set<Material> hiddenMaterials = new HashSet<>();

	private WeightedRandom<Material> randomMaterials = new WeightedRandom<Material>();

	public void serialize(ConfigurationSection section) {
		this.enabled = section.getBoolean("enabled", true);

		this.serializeWorldList(section, this.worlds, "worlds");
		if (this.worlds.isEmpty()) {
			this.failSerialize(
					String.format("config section '%s.worlds' is missing or empty", section.getCurrentPath()));
			return;
		}

		this.serializeMaterialSet(section, this.darknessMaterials, "darknessMaterials");
		this.serializeMaterialSet(section, this.hiddenMaterials, "hiddenMaterials");
		if (this.darknessMaterials.isEmpty() && this.hiddenMaterials.isEmpty()) {
			this.failSerialize(String.format("config section '%s' is missing 'darknessMaterials' and 'hiddenMaterials'",
					section.getCurrentPath()));
			return;
		}

		this.serializeRandomMaterialList(section, this.randomMaterials, "randomBlocks");
		if (this.randomMaterials.isEmpty()) {
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
			Material material = ConfigParser.getMaterialByName(materialName.toUpperCase());

			if (material == null) {
				Orebfuscator.LOGGER.warning(String.format("config section '%s.%s' contains unknown material '%s'",
						section.getCurrentPath(), path, materialName));
				continue;
			}

			materials.add(material);
		}
	}

	public void serializeRandomMaterialList(ConfigurationSection section, WeightedRandom<Material> randomMaterials,
			String path) {
		this.randomMaterials.clear();

		ConfigurationSection materialSection = section.getConfigurationSection(path);
		if (materialSection == null) {
			return;
		}

		Set<String> materialNames = materialSection.getKeys(false);
		if (materialNames.isEmpty()) {
			return;
		}

		for (String materialName : materialNames) {
			Material material = ConfigParser.getMaterialByName(materialName);

			if (material == null) {
				Orebfuscator.LOGGER.warning(String.format("config section '%s.%s' contains unknown material '%s'",
						section.getCurrentPath(), path, materialName));
				continue;
			}

			if (materialSection.isInt(materialName)) {
				randomMaterials.add(materialSection.getInt(materialName, 1), material);
			} else {
				randomMaterials.add(1, material);
			}
		}
	}

	private void failSerialize(String message) {
		this.enabled = false;
		Orebfuscator.LOGGER.warning(message);
	}

	@Override
	public Material randomMaterial() {
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
	public Set<Material> darknessMaterials() {
		return Collections.unmodifiableSet(this.darknessMaterials);
	}

	@Override
	public Set<Material> hiddenMaterials() {
		return Collections.unmodifiableSet(this.hiddenMaterials);
	}
}
