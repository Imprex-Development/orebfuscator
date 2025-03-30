package net.imprex.orebfuscator.chunk;

import net.imprex.orebfuscator.util.MinecraftVersion;

public final class ChunkCapabilities {

	// hasLongArrayLengthField < 1.21.5
	// hasChunkPosFieldUnloadPacket >= 1.20.2
	// hasClientboundLevelChunkPacketData >= 1.18;
	// hasBiomePalettedContainer >= 1.18
	// hasSingleValuePalette >= 1.18
	// hasHeightBitMask < 1.18
	// hasDynamicHeight >= 1.17

	private static final boolean hasLongArrayLengthField = MinecraftVersion.isBelow("1.21.5");
	private static final boolean hasChunkPosFieldUnloadPacket = MinecraftVersion.isAtOrAbove("1.20.2");
	private static final boolean hasClientboundLevelChunkPacketData = MinecraftVersion.isAtOrAbove("1.18");
	private static final boolean hasBiomePalettedContainer = MinecraftVersion.isAtOrAbove("1.18");
	private static final boolean hasSingleValuePalette = MinecraftVersion.isAtOrAbove("1.18");
	private static final boolean hasHeightBitMask = MinecraftVersion.isBelow("1.18");
	private static final boolean hasDynamicHeight = MinecraftVersion.isAtOrAbove("1.17");

	private ChunkCapabilities() {
	}

	public static boolean hasLongArrayLengthField() {
		return hasLongArrayLengthField;
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
}
