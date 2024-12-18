/*
 * This code is adapted from the Bukkit project:
 * https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse/src/main/java/org/bukkit/configuration/MemorySection.java
 * Copyright (C) 2011-2024 Bukkit Project (original authors and contributors)
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package net.imprex.orebfuscator.config.yaml;

import java.util.*;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigurationSection {

	private static final char PATH_SEPARATOR = '.';

	private final Map<String, Object> map = new LinkedHashMap<String, Object>();
	private final String path;

	protected ConfigurationSection(@NotNull String path) {
		Objects.requireNonNull(path, "Path cannot be null");

		this.path = path;
	}

	@NotNull
	public String getName() {
		return path;
	}

	@NotNull
	public Set<String> getKeys(boolean deep) {
		if (deep) {
			throw new UnsupportedOperationException();
		}
		return this.map.keySet();
	}

	@NotNull
	public Collection<Object> getValues(boolean deep) {
		if (deep) {
			throw new UnsupportedOperationException();
		}
		return this.map.values();
	}

	public boolean contains(@NotNull String path) {
		return get(path, null) != null;
	}

	public void set(@NotNull String path, @Nullable Object value) {
		Objects.requireNonNull(path, "Path cannot be null");

		int end = -1, start;
		ConfigurationSection section = this;
		while ((end = path.indexOf(PATH_SEPARATOR, start = end + 1)) != -1) {
			String segment = path.substring(start, end);
			ConfigurationSection subSection = section.getSection(segment);
			if (subSection == null) {
				if (value == null) {
					// no need to create missing sub-sections if we want to remove the value:
					return;
				}
				section = section.createSection(segment);
			} else {
				section = subSection;
			}
		}

		String segment = path.substring(start);
		if (section == this) {
			if (value == null) {
				map.remove(segment);
			} else {
				map.put(segment, value);
			}
		} else {
			section.set(segment, value);
		}
	}

	@Nullable
	public Object get(@NotNull String path) {
		return get(path, null);
	}

	@Nullable
	@Contract("_, !null -> !null")
	public Object get(@NotNull String path, @Nullable Object defaultValue) {
		Objects.requireNonNull(path, "Path cannot be null");

		if (path.trim().isEmpty()) {
			return this;
		}

		int end = -1, start;
		ConfigurationSection section = this;
		while ((end = path.indexOf(PATH_SEPARATOR, start = end + 1)) != -1) {
			section = section.getSection(path.substring(start, end));
			if (section == null) {
				return defaultValue;
			}
		}

		String segment = path.substring(start);
		if (section == this) {
			Object result = map.get(segment);
			return (result == null) ? defaultValue : result;
		}
		return section.get(segment, defaultValue);
	}

	@NotNull
	public ConfigurationSection createSection(@NotNull String path) {
		Objects.requireNonNull(path, "Cannot create section at empty path");

		int end = -1, start;
		ConfigurationSection section = this;
		while ((end = path.indexOf(PATH_SEPARATOR, start = end + 1)) != -1) {
			String segment = path.substring(start, end);
			ConfigurationSection subSection = section.getSection(segment);
			if (subSection == null) {
				section = section.createSection(segment);
			} else {
				section = subSection;
			}
		}

		String segment = path.substring(start);
		if (section == this) {
			ConfigurationSection result = new ConfigurationSection(segment);
			map.put(segment, result);
			return result;
		}
		return section.createSection(segment);
	}

	public boolean isBoolean(@NotNull String path) {
		return get(path) instanceof Boolean;
	}

	@Nullable
	public Boolean getBoolean(@NotNull String path) {
		return getBoolean(path, null);
	}

	@Nullable
	@Contract("_, !null -> !null")
	public Boolean getBoolean(@NotNull String path, Boolean defaultValue) {
		return get(path, defaultValue) instanceof Boolean value
				? value : defaultValue;
	}

	public boolean isInt(@NotNull String path) {
		return get(path) instanceof Integer;
	}

	@Nullable
	public Integer getInt(@NotNull String path) {
		return getInt(path, null);
	}

	@Nullable
	@Contract("_, !null -> !null")
	public Integer getInt(@NotNull String path, Integer defaultValue) {
		return get(path, defaultValue) instanceof Number value
				? value.intValue() : defaultValue;
	}

	public boolean isLong(@NotNull String path) {
		return get(path) instanceof Long;
	}

	@Nullable
	public Long getLong(@NotNull String path) {
		return getLong(path, 0L);
	}

	@Nullable
	@Contract("_, !null -> !null")
	public Long getLong(@NotNull String path, Long defaultValue) {
		return get(path, defaultValue) instanceof Number value
				? value.longValue() : defaultValue;
	}

	public boolean isDouble(@NotNull String path) {
		return get(path) instanceof Double;
	}

	@Nullable
	public Double getDouble(@NotNull String path) {
		return getDouble(path, 0.0D);
	}

	@Nullable
	@Contract("_, !null -> !null")
	public Double getDouble(@NotNull String path, Double defaultValue) {
		return get(path, defaultValue) instanceof Number value
				? value.doubleValue() : defaultValue;
	}

	public boolean isString(@NotNull String path) {
		return get(path) instanceof String;
	}

	@Nullable
	public String getString(@NotNull String path) {
		return getString(path, null);
	}

	@Nullable
	@Contract("_, !null -> !null")
	public String getString(@NotNull String path, String defaultValue) {
		return get(path, defaultValue) instanceof String value
				? value : defaultValue;
	}

	public boolean isList(@NotNull String path) {
		return get(path) instanceof List;
	}

	@Nullable
	public List<?> getList(@NotNull String path) {
		return getList(path, null);
	}

	@Nullable
	@Contract("_, !null -> !null")
	public List<?> getList(@NotNull String path, List<?> defaultValue) {
		return get(path, defaultValue) instanceof List<?> value
				? value : defaultValue;
	}

    @NotNull
    public List<String> getStringList(@NotNull String path) {
        List<?> list = getList(path);
        if (list == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();

        for (Object object : list) {
            if ((object instanceof String) || (isPrimitiveWrapper(object))) {
                result.add(String.valueOf(object));
            }
        }

        return result;
    }

	public boolean isSection(@NotNull String path) {
		return get(path) instanceof ConfigurationSection;
	}

	@Nullable
	public ConfigurationSection getSection(@NotNull String path) {
		return get(path) instanceof ConfigurationSection value
				? value : null;
	}

	@NotNull
	public List<ConfigurationSection> getSubSections() {
		List<ConfigurationSection> result = new ArrayList<>();

		for (Object value : this.map.values()) {
			if (value instanceof ConfigurationSection subSection) {
				result.add(subSection);
			}
		}

		return result;
	}

    private static boolean isPrimitiveWrapper(@Nullable Object input) {
        return input instanceof Integer || input instanceof Boolean
                || input instanceof Character || input instanceof Byte
                || input instanceof Short || input instanceof Double
                || input instanceof Long || input instanceof Float;
    }
}
