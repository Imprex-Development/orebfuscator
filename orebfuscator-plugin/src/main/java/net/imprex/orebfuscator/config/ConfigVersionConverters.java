package net.imprex.orebfuscator.config;

import net.imprex.orebfuscator.util.BlockPos;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ConfigVersionConverters {

	private static final Map<Integer, Function<ConfigurationSection, ConfigurationSection>> CONVERTER = new HashMap<>();

	static {
		CONVERTER.put(1, (section) -> {
			// check if config is still using old path
			String obfuscationConfigPath = section.contains("world") ? "world" : "obfuscation";
			ConfigVersionConverters.convertSectionListToSection(section, obfuscationConfigPath);
			ConfigVersionConverters.convertSectionListToSection(section, "proximity");
			section.set("version", 2);
			return section;
		});

		CONVERTER.put(2, (section) -> {
			convertRandomBlocksToSections(section.getConfigurationSection("obfuscation"));
			convertRandomBlocksToSections(section.getConfigurationSection("proximity"));
			section.set("version", 3);
			return section;
		});
	}

	public static void convertToLatestVersion(ConfigurationSection section) {
		while (true) {
			int configVersion = section.getInt("version", -1);

			Function<ConfigurationSection, ConfigurationSection> converter = CONVERTER.get(configVersion);
			if (converter == null) {
				break;
			}

			section = converter.apply(section);
		}
	}

	private static void convertSectionListToSection(ConfigurationSection parentSection, String path) {
		List<ConfigurationSection> sections = deserializeSectionList(parentSection, path);
		ConfigurationSection section = parentSection.createSection(path);
		for (ConfigurationSection childSection : sections) {
			section.set(childSection.getName(), childSection);
		}
	}

	private static List<ConfigurationSection> deserializeSectionList(ConfigurationSection parentSection, String path) {
		List<ConfigurationSection> sections = new ArrayList<>();

		List<?> sectionList = parentSection.getList(path);
		if (sectionList != null) {
			for (int i = 0; i < sectionList.size(); i++) {
				Object section = sectionList.get(i);
				if (section instanceof Map) {
					sections.add(ConfigVersionConverters.convertMapsToSections((Map<?, ?>) section,
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

	private static void convertRandomBlocksToSections(ConfigurationSection parentSection) {
		for (String key : parentSection.getKeys(false)) {
			ConfigurationSection obfuscation = parentSection.getConfigurationSection(key);
			ConfigurationSection blockSection = obfuscation.getConfigurationSection("randomBlocks");
			if (blockSection == null) {
				continue;
			}

			ConfigurationSection newBlockSection = obfuscation.createSection("randomBlocks");
			newBlockSection = newBlockSection.createSection("section-global");
			newBlockSection.set("minY", BlockPos.MIN_Y);
			newBlockSection.set("maxY", BlockPos.MAX_Y);
			newBlockSection = newBlockSection.createSection("blocks");

			for (String blockName : blockSection.getKeys(false)) {
				newBlockSection.set(blockName, blockSection.getInt(blockName, 1));
			}
		}
	}
}
