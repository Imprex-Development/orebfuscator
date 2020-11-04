package net.imprex.orebfuscator.config;

public interface BlockMask {

	public static final int FLAG_OBFUSCATE = 1;
	public static final int FLAG_TILE_ENTITY = 2;
	public static final int FLAG_PROXIMITY = 4;
	public static final int FLAG_USE_BLOCK_BELOW = 8;

	public static boolean isEmpty(int mask) {
		return (mask & 0x7F) == 0;
	}

	public static boolean isSet(int mask, int flag) {
		return (mask & flag) != 0;
	}

	public static boolean isObfuscateSet(int mask) {
		return isSet(mask, FLAG_OBFUSCATE);
	}

	public static boolean isTileEntitySet(int mask) {
		return isSet(mask, FLAG_TILE_ENTITY);
	}

	public static boolean isProximitySet(int mask) {
		return isSet(mask, FLAG_PROXIMITY);
	}

	public static boolean isUseBlockBelowSet(int mask) {
		return isSet(mask, FLAG_USE_BLOCK_BELOW);
	}

	int mask(int blockId);

	int mask(int blockId, int y);
}
