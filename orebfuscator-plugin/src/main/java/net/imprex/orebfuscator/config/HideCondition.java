package net.imprex.orebfuscator.config;

public class HideCondition {

	public static final short EMPTY = HideCondition.create(0, true);

	public static short create(int y, boolean above) {
		return (short) ((y & 0xFF) << 8 | (above ? 0x80 : 0x00));
	}

	public static boolean match(short hideCondition, int y) {
		int expectedY = hideCondition >> 8;
		if ((hideCondition & 0x80) != 0) {
			return expectedY < y;
		} else {
			return expectedY > y;
		}
	}

	public static int getY(short hideCondition) {
		return hideCondition >> 8;
	}

	public static boolean getAbove(short hideCondition) {
		return (hideCondition & 0x80) != 0;
	}
}
