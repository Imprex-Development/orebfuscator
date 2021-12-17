package net.imprex.orebfuscator.config;

import net.imprex.orebfuscator.util.BlockPos;

/**
 * Only use 25 MSBs of blockFlags for ProximityHeightCondition
 * 12 bit min y | 12 bit max y | 1 bit present
 */
public class ProximityHeightCondition {

	public static final int MATCH_ALL = ProximityHeightCondition.create(BlockPos.MIN_Y, BlockPos.MAX_Y);

	public static int clampY(int y) {
		return Math.min(BlockPos.MAX_Y, Math.max(BlockPos.MIN_Y, y));
	}

	public static int create(int minY, int maxY) {
		return clampY(minY) << 20 | clampY(maxY) << 8 | 0x80;
	}

	public static int remove(int hideCondition) {
		return hideCondition & 0x7F;
	}

	private static int extractHideCondition(int hideCondition) {
		return hideCondition & 0xFFFFFF80;
	}

	public static boolean equals(int a, int b) {
		return extractHideCondition(a) == extractHideCondition(b);
	}

	public static boolean match(int hideCondition, int y) {
		return isPresent(hideCondition) && getMinY(hideCondition) <= y && getMaxY(hideCondition) >= y;
	}

	public static boolean isPresent(int hideCondition) {
		return (hideCondition & 0x80) != 0;
	}

	public static int getMinY(int hideCondition) {
		return hideCondition >> 20;
	}

	public static int getMaxY(int hideCondition) {
		return hideCondition << 12 >> 20;
	}
}
