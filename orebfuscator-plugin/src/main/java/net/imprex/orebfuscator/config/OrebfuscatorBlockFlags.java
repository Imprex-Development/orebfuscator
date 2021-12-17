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
		for (int blockState : NmsInstance.getBlockIds(material)) {
			int blockMask = this.blockFlags[blockState] | bits;

			if (NmsInstance.isBlockEntity(blockState)) {
				blockMask |= FLAG_BLOCK_ENTITY;
			}

			this.blockFlags[blockState] = blockMask;
		}
	}

	@Override
	public int flags(int blockState) {
		return this.blockFlags[blockState];
	}

	@Override
	public int flags(int blockState, int y) {
		int flags = this.blockFlags[blockState];
		if (ProximityHeightCondition.match(flags, y)) {
			flags |= FLAG_PROXIMITY;
		}
		return flags;
	}
}
