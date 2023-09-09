package net.imprex.orebfuscator.config;

import java.util.Map.Entry;

import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.BlockStateProperties;

public class OrebfuscatorBlockFlags implements BlockFlags {

	private static final OrebfuscatorBlockFlags EMPTY_FLAGS = new OrebfuscatorBlockFlags(null, null);

	static OrebfuscatorBlockFlags create(OrebfuscatorObfuscationConfig worldConfig, OrebfuscatorProximityConfig proximityConfig) {
		if ((worldConfig != null && worldConfig.isEnabled()) || (proximityConfig != null && proximityConfig.isEnabled())) {
			return new OrebfuscatorBlockFlags(worldConfig, proximityConfig);
		}
		return EMPTY_FLAGS;
	}

	private final int[] blockFlags = new int[OrebfuscatorNms.getTotalBlockCount()];

	private OrebfuscatorBlockFlags(OrebfuscatorObfuscationConfig worldConfig, OrebfuscatorProximityConfig proximityConfig) {
		if (worldConfig != null && worldConfig.isEnabled()) {
			for (BlockProperties block : worldConfig.hiddenBlocks()) {
				this.setBlockBits(block, FLAG_OBFUSCATE);
			}
		}

		if (proximityConfig != null && proximityConfig.isEnabled()) {
			for (Entry<BlockProperties, Integer> entry : proximityConfig.hiddenBlocks()) {
				this.setBlockBits(entry.getKey(), entry.getValue());
			}
			for (BlockProperties block : proximityConfig.allowForUseBlockBelow()) {
				this.setBlockBits(block, FLAG_ALLOW_FOR_USE_BLOCK_BELOW);
			}
		}
	}

	private void setBlockBits(BlockProperties block, int bits) {
		for (BlockStateProperties blockState : block.getPossibleBlockStates()) {
			int blockMask = this.blockFlags[blockState.getId()] | bits;

			if (blockState.isBlockEntity()) {
				blockMask |= FLAG_BLOCK_ENTITY;
			}

			this.blockFlags[blockState.getId()] = blockMask;
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
