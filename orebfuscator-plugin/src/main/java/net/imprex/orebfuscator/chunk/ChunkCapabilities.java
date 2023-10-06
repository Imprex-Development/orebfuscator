package net.imprex.orebfuscator.chunk;

import net.imprex.orebfuscator.util.MinecraftVersion;

public final class ChunkCapabilities {

	// hasChunkPosFieldUnloadPacket >= 1.20.2
	// hasClientboundLevelChunkPacketData >= 1.18;
	// hasBiomePalettedContainer >= 1.18
	// hasSingleValuePalette >= 1.18
	// hasHeightBitMask <= 1.17
	// hasDynamicHeight >= 1.17
	// hasSimpleVarBitBuffer >= 1.16
	// hasBlockCount >= 1.14
	// hasDirectPaletteZeroLength < 1.13
	// hasLight < 1.14

	private static final boolean hasChunkPosFieldUnloadPacket = MinecraftVersion.minorVersion() > 20 ||
			(MinecraftVersion.minorVersion() == 20 && MinecraftVersion.revisionNumber() >= 2);
	private static final boolean hasClientboundLevelChunkPacketData = MinecraftVersion.minorVersion() >= 18;
	private static final boolean hasBiomePalettedContainer = MinecraftVersion.minorVersion() >= 18;
	private static final boolean hasSingleValuePalette = MinecraftVersion.minorVersion() >= 18;
	private static final boolean hasHeightBitMask = MinecraftVersion.minorVersion() <= 17;
	private static final boolean hasDynamicHeight = MinecraftVersion.minorVersion() >= 17;
	private static final boolean hasSimpleVarBitBuffer = MinecraftVersion.minorVersion() >= 16;
	private static final boolean hasBlockCount = MinecraftVersion.minorVersion() >= 14;
	private static final boolean hasDirectPaletteZeroLength = MinecraftVersion.minorVersion() < 13;
	private static final boolean hasLightArray = MinecraftVersion.minorVersion() < 14;

	private ChunkCapabilities() {
	}

	public static boolean hasChunkPosFieldUnloadPacket() {
		return hasChunkPosFieldUnloadPacket;
	}

	public static boolean hasClientboundLevelChunkPacketData() {
		return hasClientboundLevelChunkPacketData;
	}

	public static boolean hasBiomePalettedContainer() {
		return hasBiomePalettedContainer;
	}

	public static boolean hasSingleValuePalette() {
		return hasSingleValuePalette;
	}

	public static boolean hasHeightBitMask() {
		return hasHeightBitMask;
	}

	public static boolean hasDynamicHeight() {
		return hasDynamicHeight;
	}

	public static boolean hasSimpleVarBitBuffer() {
		return hasSimpleVarBitBuffer;
	}

	public static boolean hasBlockCount() {
		return hasBlockCount;
	}

	public static boolean hasDirectPaletteZeroLength() {
		return hasDirectPaletteZeroLength;
	}

	public static boolean hasLightArray() {
		return hasLightArray;
	}

	public static int getExtraBytes(ChunkStruct chunkStruct) {
		int extraBytes = ChunkCapabilities.hasLightArray() ? 2048 : 0;
		if (chunkStruct.isOverworld) {
			extraBytes *= 2;
		}
		return extraBytes;
	}

	public static VarBitBuffer createVarBitBuffer(int bitsPerEntry, int size) {
		if (hasSimpleVarBitBuffer) {
			return new SimpleVarBitBuffer(bitsPerEntry, size);
		}
		return new CompactVarBitBuffer(bitsPerEntry, size);
	}
}
