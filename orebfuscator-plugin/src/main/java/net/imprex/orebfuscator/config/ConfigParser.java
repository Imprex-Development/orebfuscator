package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.OFCLogger;

public class ConfigParser {

	public static List<ConfigurationSection> serializeSectionList(ConfigurationSection parentSection, String path) {
		List<ConfigurationSection> sections = new ArrayList<>();

		List<?> sectionList = parentSection.getList(path);
		if (sectionList != null) {
			for (int i = 0; i < sectionList.size(); i++) {
				Object section = sectionList.get(i);
				if (section instanceof Map) {
					sections.add(ConfigParser.convertMapsToSections((Map<?, ?>) section,
							parentSection.createSection(path + "-" + i)));
				}
			}
		}

		return sections;
	}

	private static ConfigurationSection convertMapsToSections(Map<?, ?> input, ConfigurationSection section) {
		for (Map.Entry<?, ?> entry : input.entrySet()) {
			String key = entry.getKey().toString();
			Object value = entry.getValue();

			if (value instanceof Map) {
				convertMapsToSections((Map<?, ?>) value, section.createSection(key));
			} else {
				section.set(key, value);
			}
		}
		return section;
	}

	public static void serializeRandomMaterialList(ConfigurationSection section, Map<Material, Integer> randomBlocks,
			String path) {
		ConfigurationSection materialSection = section.getConfigurationSection(path);
		if (materialSection == null) {
			return;
		}

		for (String name : materialSection.getKeys(false)) {
			Optional<Material> material = NmsInstance.getMaterialByName(name);

			if (!material.isPresent()) {
				OFCLogger.warn(String.format("config section '%s.%s' contains unknown block '%s'",
						section.getCurrentPath(), path, name));
				continue;
			}

			int weight = materialSection.isInt(name) ? materialSection.getInt(name, 1) : 1;
			randomBlocks.put(material.get(), weight);
		}
	}

	public static void deserializeRandomMaterialList(ConfigurationSection section, Map<Material, Integer> randomBlocks,
			String path) {
		ConfigurationSection materialSection = section.createSection(path);

		for (Entry<Material, Integer> entry : randomBlocks.entrySet()) {
			Material material = entry.getKey();
			Optional<String> optional = NmsInstance.getNameByMaterial(material);

			if (!optional.isPresent()) {
				OFCLogger.warn(String.format("config section '%s.%s' contains unknown block name '%s'",
						section.getCurrentPath(), path, material != null ? material.name() : null));
				continue;
			}

			materialSection.set(optional.get(), entry.getValue());
		}
	}

	public static void serializeMaterialSet(ConfigurationSection section, Set<Material> materials, String path) {
		for (String materialName : section.getStringList(path)) {
			Optional<Material> material = NmsInstance.getMaterialByName(materialName);

			if (!material.isPresent()) {
				OFCLogger.warn(String.format("config section '%s.%s' contains unknown block '%s'",
						section.getCurrentPath(), path, materialName));
				continue;
			}

			materials.add(material.get());
		}
	}

	public static void deserializeMaterialSet(ConfigurationSection section, Set<Material> materials, String path) {
		List<String> materialNames = new ArrayList<>();
		for (Material material : materials) {
			Optional<String> optional = NmsInstance.getNameByMaterial(material);

			if (!optional.isPresent()) {
				OFCLogger.warn(String.format("config section '%s.%s' contains unknown block name '%s'",
						section.getCurrentPath(), path, material != null ? material.name() : null));
				continue;
			}

			materialNames.add(optional.get());
		}

		section.set(path, materialNames);
	}
}
