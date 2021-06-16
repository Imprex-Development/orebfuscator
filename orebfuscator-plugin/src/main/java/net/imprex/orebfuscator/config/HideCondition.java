package net.imprex.orebfuscator.config;

/**
 * Only use MSBs (24 bit) for HideCondition
 * 16 bit y | 1 bit above flag | 1 present bit
 */
public class HideCondition {

	public static final int MATCH_ALL = HideCondition.create(Short.MIN_VALUE, true);

	public static int clampY(int y) {
		return Math.min(Short.MAX_VALUE, Math.max(Short.MIN_VALUE, y));
	}

	public static int create(int y, boolean above) {
		return (clampY(y) << 16 | (above ? 0xC000 : 0x4000));
	}

	public static int remove(int hideCondition) {
		return hideCondition & 0xFFF;
	}

	private static int extractHideCondition(int hideCondition) {
		return hideCondition & 0xFFFFF000;
	}

	public static boolean equals(int a, int b) {
		return extractHideCondition(a) == extractHideCondition(b);
	}

	public static boolean match(int hideCondition, int y) {
		if (isPresent(hideCondition)) {
			int expectedY = getY(hideCondition);
			if (getAbove(hideCondition)) {
				return expectedY < y;
			} else {
				return expectedY > y;
			}
		}
		return false;
	}

	public static boolean isPresent(int hideCondition) {
		return (hideCondition & 0x4000) != 0;
	}

	public static int getY(int hideCondition) {
		return (short) (hideCondition >> 16);
	}

	public static boolean getAbove(int hideCondition) {
		return (hideCondition & 0x8000) != 0;
	}
}
