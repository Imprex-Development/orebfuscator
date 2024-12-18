/*
 * This code is adapted from the Bukkit project:
 * https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse/src/main/java/org/bukkit/configuration/file/YamlConfiguration.java
 * Copyright (C) 2011-2024 Bukkit Project (original authors and contributors)
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package net.imprex.orebfuscator.config.yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

public class YamlConfiguration extends ConfigurationSection {

	private final DumperOptions yamlOptions = new DumperOptions();
	private final LoaderOptions loaderOptions = new LoaderOptions();
	private final Representer yamlRepresenter = new YamlRepresenter(this.yamlOptions);
	private final Yaml yaml = new Yaml(new Constructor(), yamlRepresenter, yamlOptions, loaderOptions);

	protected YamlConfiguration() {
		super("");

		yamlOptions.setIndent(2);
		yamlOptions.setWidth(80);
		yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
	}

	public void save(@NotNull Path path) throws IOException {
		Objects.requireNonNull(path, "Path cannot be null");

		Files.createDirectories(path.getParent());

		String data = yaml.dump(getValues(false));
		try (Writer writer = Files.newBufferedWriter(path)) {
			writer.write(data);
		}
	}

	private void loadFromString(@NotNull String contents) throws InvalidConfigurationException {
		Objects.requireNonNull(contents, "Contents cannot be null");

		Map<?, ?> input;
		try {
			input = (Map<?, ?>) yaml.load(contents);
		} catch (YAMLException e) {
			throw new InvalidConfigurationException(e);
		} catch (ClassCastException e) {
			throw new InvalidConfigurationException("Top level is not a Map.");
		}

		if (input != null) {
			convertMapsToSections(input, this);
		}
	}

	private static void convertMapsToSections(@NotNull Map<?, ?> input, @NotNull ConfigurationSection section) {
		for (Map.Entry<?, ?> entry : input.entrySet()) {
			String key = entry.getKey().toString();
			Object value = entry.getValue();

			if (value instanceof Map<?, ?> map) {
				convertMapsToSections(map, section.createSection(key));
			} else {
				section.set(key, value);
			}
		}
	}

	@NotNull
	public static YamlConfiguration loadConfig(@NotNull Path path) throws IOException, InvalidConfigurationException {
		Objects.requireNonNull(path, "Path cannot be null");

		YamlConfiguration config = new YamlConfiguration();

		try (BufferedReader reader = Files.newBufferedReader(path)) {
			StringBuilder builder = new StringBuilder();

			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append('\n');
			}

			config.loadFromString(builder.toString());
		}

		return config;
	}
}
