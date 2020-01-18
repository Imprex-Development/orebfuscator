package net.imprex.orebfuscator.util;

import java.util.BitSet;

import org.bukkit.Material;

import net.imprex.orebfuscator.NmsInstance;

public class MaterialUtil {

	private static final BitSet TRANSPARENT_BLOCKS = new BitSet();

	static {
		for (Material material : Material.values()) {
			if (material.isBlock() && !material.isOccluding()) {
				for (int id : NmsInstance.get().getMaterialIds(material)) {
					TRANSPARENT_BLOCKS.set(id);
				}
			}
		}
	}

	public static boolean isTransparent(int id) {
		return TRANSPARENT_BLOCKS.get(id);
	}
}
