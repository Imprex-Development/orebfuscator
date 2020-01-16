package net.imprex.orebfuscator.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import com.lishid.orebfuscator.Orebfuscator;

import net.imprex.orebfuscator.util.WeightedRandom;

public class ConfigParser {

	private static final Map<String, Material> MATERIAL_BY_NAME = new HashMap<>();

	static {
		for (Material material : Material.values()) {
			MATERIAL_BY_NAME.put(material.name(), material);
		}
	}

	public static Material getMaterialByName(String materialName) {
		return MATERIAL_BY_NAME.get(materialName.toUpperCase());
	}

	public static void readWorldList(ConfigurationSection section, List<World> worlds, String path) {
		List<String> worldNameList = section.getStringList(path);

		if (worldNameList == null || worldNameList.isEmpty()) {
			return;
		}

		for (String worldName : worldNameList) {
			World world = Bukkit.getWorld(worldName);

			if (world == null) {
				Orebfuscator.LOGGER.warning("config '" + section.getCurrentPath() + "' world '" + worldName + "' in '" + path + "' couldn't be found");
				continue;
			}

			worlds.add(world);
		}
	}

	public static void readMaterialSet(ConfigurationSection section, Set<Material> materials, String path) {
		List<String> materialNameList = section.getStringList(path);

		if (materialNameList == null || materialNameList.isEmpty()) {
			return;
		}

		for (String materialName : materialNameList) {
			Material material = getMaterialByName(materialName.toUpperCase());

			if (material == null) {
				Orebfuscator.LOGGER.warning("config '" + section.getCurrentPath() + "' material '" + materialName + "' in '" + path + "' couldn't be found");
				continue;
			}

			materials.add(material);
		}
	}

	public static void readRandomMaterialList(ConfigurationSection section, WeightedRandom<Material> randomMaterials, String path) {
		ConfigurationSection materialSection = section.getConfigurationSection(path);
		if (materialSection == null) {
			return;
		}

		Set<String> materialNames = materialSection.getKeys(false);
		if (materialNames.isEmpty()) {
			return;
		}

		for (String materialName : materialNames) {
			Material material = getMaterialByName(materialName);

			if (material == null) {
				Orebfuscator.LOGGER.warning("config section '" + section.getCurrentPath() + "' material '" + materialName + "' in '" + path + "' couldn't be found");
				continue;
			}

			if (materialSection.isInt(materialName)) {
				randomMaterials.add(materialSection.getInt(materialName, 1), material);
			} else {
				randomMaterials.add(1, material);
			}
		}
	}
}
