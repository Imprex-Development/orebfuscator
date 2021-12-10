package net.imprex.orebfuscator.config;

import java.util.Map.Entry;

import org.bukkit.Material;

import net.imprex.orebfuscator.NmsInstance;

public class OrebfuscatorBlockFlags implements BlockFlags {

	private static final OrebfuscatorBlockFlags EMPTY_FLAGS = new OrebfuscatorBlockFlags(null, null);

	static OrebfuscatorBlockFlags create(OrebfuscatorObfuscationConfig worldConfig, OrebfuscatorProximityConfig proximityConfig) {
		if ((worldConfig != null && worldConfig.isEnabled()) || (proximityConfig != null && proximityConfig.isEnabled())) {
			return new OrebfuscatorBlockFlags(worldConfig, proximityConfig);
		}
		return EMPTY_FLAGS;
	}

	private final int[] blockFlags = new int[NmsInstance.getTotalBlockCount()];

	private OrebfuscatorBlockFlags(OrebfuscatorObfuscationConfig worldConfig, OrebfuscatorProximityConfig proximityConfig) {
		if (worldConfig != null && worldConfig.isEnabled()) {
			for (Material material : worldConfig.hiddenBlocks()) {
				this.setBlockBits(material, FLAG_OBFUSCATE);
			}
		}
		if (proximityConfig != null && proximityConfig.isEnabled()) {
			for (Entry<Material, Integer> entry : proximityConfig.hiddenBlocks()) {
				this.setBlockBits(entry.getKey(), entry.getValue());
			}
		}
	}

	private void setBlockBits(Material material, int bits) {
		for (int blockId : NmsInstance.getBlockIds(material)) {
			int blockMask = this.blockFlags[blockId] | bits;

			if (NmsInstance.isTileEntity(blockId)) {
				blockMask |= FLAG_TILE_ENTITY;
			}

			this.blockFlags[blockId] = blockMask;
		}
	}

	@Override
	public int flags(int blockId) {
		return this.blockFlags[blockId];
	}

	@Override
	public int flags(int blockId, int y) {
		int blockFlags = this.blockFlags[blockId];
		if (HideCondition.match(blockFlags, y)) {
			blockFlags |= FLAG_PROXIMITY;
		}
		return blockFlags;
	}
}
