/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.utils;

import java.util.HashMap;
import java.util.Set;

import org.bukkit.Material;

import net.imprex.orebfuscator.NmsInstance;

public class MaterialHelper {

	private static final HashMap<Integer, Material> BLOCK_BY_ID = new HashMap<Integer, Material>();

	public static void initialize() {
		Material[] allMaterials = Material.values();

		for (Material material : allMaterials) {
			if (material.isBlock()) {
				Set<Integer> ids = NmsInstance.get().getMaterialIds(material);

				for (int id : ids) {
					MaterialHelper.BLOCK_BY_ID.put(id, material);
				}
			}
		}
	}

	public static Material getById(int combinedBlockId) {
		return MaterialHelper.BLOCK_BY_ID.get(combinedBlockId);
	}

	public static int getMaxId() {
		int maxId = -1;

		for (int id : MaterialHelper.BLOCK_BY_ID.keySet()) {
			if (id > maxId) {
				maxId = id;
			}
		}

		return maxId;
	}
}
