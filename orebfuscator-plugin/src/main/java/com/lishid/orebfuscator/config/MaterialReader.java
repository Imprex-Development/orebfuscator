/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.utils.Globals;
import com.lishid.orebfuscator.utils.MaterialHelper;

import net.imprex.orebfuscator.NmsInstance;

public class MaterialReader {

	private static class MaterialResult {
		public Set<Integer> ids;
		public String name;

		public MaterialResult(Set<Integer> ids, String name) {
			this.ids = ids;
			this.name = name;
		}
	}

	private JavaPlugin plugin;

	public MaterialReader(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	private FileConfiguration getConfig() {
		return this.plugin.getConfig();
	}

	public Set<Integer> getMaterialIds(String materialName) {
		return this.getMaterial(materialName, null).ids;
	}

	public Integer getMaterialIdByPath(String path, Integer defaultMaterialId, boolean withSave) {
		boolean hasKey = this.getConfig().get(path) != null;

		if (!hasKey && defaultMaterialId == null) {
			return null;
		}

		String materialName = hasKey ? this.getConfig().getString(path) : Integer.toString(defaultMaterialId);
		MaterialResult material = this.getMaterial(materialName, defaultMaterialId);

		if (withSave || hasKey) {
			this.getConfig().set(path, material.name);
		}

		return material.ids.iterator().next();
	}

	public Integer[] getMaterialIdsByPath(String path, Integer[] defaultMaterials, boolean withSave) {
		List<String> list;

		if (this.getConfig().get(path) != null) {
			list = this.getConfig().getStringList(path);
			withSave = true;
		} else {
			if (defaultMaterials != null) {
				list = new ArrayList<>();

				for (int materialId : defaultMaterials) {
					list.add(MaterialHelper.getById(materialId).name());
				}
			} else {
				return null;
			}
		}

		List<Integer> result = new ArrayList<>();
		HashSet<String> uniqueNames = new HashSet<>();

		for (int i = 0; i < list.size(); i++) {
			MaterialResult material = this.getMaterial(list.get(i), null);

			if (material != null) {
				uniqueNames.add(material.name);
				result.addAll(material.ids);
			}
		}

		if (withSave) {
			list.clear();
			list.addAll(uniqueNames);
			Collections.sort(list);

			this.getConfig().set(path, list);
		}

		return result.toArray(new Integer[0]);
	}

	private MaterialResult getMaterial(String inputMaterialName, Integer defaultMaterialId) {
		Set<Integer> materialIds = null;
		String defaultMaterialName = defaultMaterialId != null ? MaterialHelper.getById(defaultMaterialId).name()
				: null;
		String materialName = inputMaterialName;

		try {
			if (Character.isDigit(materialName.charAt(0))) {
				int id = Integer.parseInt(materialName);

				Material obj = MaterialHelper.getById(id);

				if (obj != null) {
					materialName = obj.name();

					materialIds = new HashSet<>();
					materialIds.add(id);
				} else {
					if (defaultMaterialId != null) {
						Orebfuscator.LOGGER.info(Globals.LOG_PREFIX + "Material with ID = " + id
								+ " is not found. Will be used default material: " + defaultMaterialName);
						materialName = defaultMaterialName;

						materialIds = new HashSet<>();
						materialIds.add(defaultMaterialId);
					} else {
						Orebfuscator.LOGGER.info(Globals.LOG_PREFIX + "Material with ID = " + id + " is not found. Skipped.");
					}
				}
			} else {
				Material obj = Material.getMaterial(materialName.toUpperCase());

				if (obj != null) {
					materialIds = NmsInstance.get().getMaterialIds(obj);
				} else {
					if (defaultMaterialId != null) {
						Orebfuscator.LOGGER.info(Globals.LOG_PREFIX + "Material " + materialName
								+ " is not found. Will be used default material: " + defaultMaterialName);
						materialName = defaultMaterialName;

						materialIds = new HashSet<>();
						materialIds.add(defaultMaterialId);
					} else {
						Orebfuscator.LOGGER.info(Globals.LOG_PREFIX + "Material " + materialName + " is not found. Skipped.");
					}
				}
			}
		} catch (Exception e) {
			if (defaultMaterialId != null) {
				Orebfuscator.LOGGER.info(Globals.LOG_PREFIX + "Invalid material ID or name: " + materialName
						+ ".  Will be used default material: " + defaultMaterialName);
				materialName = defaultMaterialName;

				materialIds = new HashSet<>();
				materialIds.add(defaultMaterialId);
			} else {
				Orebfuscator.LOGGER.info(Globals.LOG_PREFIX + "Invalid material ID or name: " + materialName + ". Skipped.");
			}
		}

		return materialIds != null ? new MaterialResult(materialIds, materialName) : null;
	}
}
