package net.imprex.orebfuscator.chunk;

public class ChunkCapabilities {

	public static boolean hasLightArray = false;
	public static boolean hasBlockCount = false;
	public static boolean hasDirectPaletteZeroLength = false;
	public static boolean hasSimpleVarBitBuffer = false;

	public static void hasLightArray() {
		hasLightArray = true;
	}

	public static void hasBlockCount() {
		hasBlockCount = true;
	}

	public static void hasDirectPaletteZeroLength() {
		hasDirectPaletteZeroLength = true;
	}

	public static void hasSimpleVarBitBuffer() {
		hasSimpleVarBitBuffer = true;
	}

	public static VarBitBuffer createVarBitBuffer(int bitsPerEntry, int size) {
		if (hasSimpleVarBitBuffer) {
			return new SimpleVarBitBuffer(bitsPerEntry, size);
		}
		return new CompactVarBitBuffer(bitsPerEntry, size);
	}
}
