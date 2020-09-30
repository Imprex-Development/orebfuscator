package net.imprex.orebfuscator.config;

import java.util.Map;

import org.bukkit.Material;

import net.imprex.orebfuscator.NmsInstance;

public class OrebfuscatorBlockMask implements BlockMask {

	static final OrebfuscatorBlockMask EMPTY_MASK = new OrebfuscatorBlockMask(null, null);

	static OrebfuscatorBlockMask create(OrebfuscatorWorldConfig worldConfig, OrebfuscatorProximityConfig proximityConfig) {
		if (worldConfig != null || proximityConfig != null) {
			return new OrebfuscatorBlockMask(worldConfig, proximityConfig);
		}
		return EMPTY_MASK;
	}

	private final short[] blockMask = new short[NmsInstance.getMaterialSize()];

	private OrebfuscatorBlockMask(OrebfuscatorWorldConfig worldConfig, OrebfuscatorProximityConfig proximityConfig) {
		if (worldConfig != null && worldConfig.enabled()) {
			for (Material material : worldConfig.hiddenBlocks()) {
				this.setBlockMask(material, BLOCK_MASK_OBFUSCATE);
			}
		}
		if (proximityConfig != null && proximityConfig.enabled()) {
			for (Map.Entry<Material, Short> entry : proximityConfig.hiddenBlocks().entrySet()) {
				this.setBlockMask(entry.getKey(), entry.getValue());
			}
		}
	}

	public void setBlockMask(Material material, int mask) {
		for (int blockId : NmsInstance.getMaterialIds(material)) {
			int blockMask = this.blockMask[blockId] | mask;

			if (NmsInstance.isTileEntity(blockId)) {
				blockMask |= BLOCK_MASK_TILEENTITY;
			}

			this.blockMask[blockId] = (short) blockMask;
		}
	}

	@Override
	public int mask(int blockId) {
		return this.blockMask[blockId];
	}

	@Override
	public int mask(int blockId, int y) {
		short blockMask = this.blockMask[blockId];
		if (HideCondition.match(blockMask, y)) {
			blockMask |= BLOCK_MASK_PROXIMITY;
		}
		return blockMask;
	}
}
