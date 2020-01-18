package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import com.lishid.orebfuscator.Orebfuscator;

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

	public static ConfigurationSection convertMapsToSections(Map<?, ?> input, ConfigurationSection section) {
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
		randomBlocks.clear();

		ConfigurationSection materialSection = section.getConfigurationSection(path);
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
				Orebfuscator.LOGGER.warning(String.format("config section '%s.%s' contains unknown block '%s'",
						section.getCurrentPath(), path, materialName));
				continue;
			}

			if (materialSection.isInt(materialName)) {
				randomBlocks.put(material, materialSection.getInt(materialName, 1));
			} else {
				randomBlocks.put(material, 1);
			}
		}
	}
}
