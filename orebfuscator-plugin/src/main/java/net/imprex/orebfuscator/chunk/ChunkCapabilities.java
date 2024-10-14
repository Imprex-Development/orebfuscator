package net.imprex.orebfuscator.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.imprex.orebfuscator.chunk.next.bitstorage.BitStorage;
import net.imprex.orebfuscator.util.MinecraftVersion;

public final class ChunkCapabilities {

	// hasChunkPosFieldUnloadPacket >= 1.20.2
	// hasClientboundLevelChunkPacketData >= 1.18;
	// hasBiomePalettedContainer >= 1.18
	// hasSingleValuePalette >= 1.18
	// hasHeightBitMask < 1.18
	// hasDynamicHeight >= 1.17

	private static final boolean hasChunkPosFieldUnloadPacket = MinecraftVersion.isAtOrAbove("1.20.2");
	private static final boolean hasClientboundLevelChunkPacketData = MinecraftVersion.isAtOrAbove("1.18");
	private static final boolean hasBiomePalettedContainer = MinecraftVersion.isAtOrAbove("1.18");
	private static final boolean hasSingleValuePalette = MinecraftVersion.isAtOrAbove("1.18");
	private static final boolean hasHeightBitMask = MinecraftVersion.isBelow("1.18");
	private static final boolean hasDynamicHeight = MinecraftVersion.isAtOrAbove("1.17");

	private static final byte[] emptyBlockPalettedContainer = createEmptyBlockPalettedContainer();

	private static byte[] createEmptyBlockPalettedContainer() {
		ByteBuf buffer = Unpooled.buffer();

		// write block count
		buffer.writeShort(0);

		if (ChunkCapabilities.hasSingleValuePalette()) {
			// write bitsPerBlock + palette
			buffer.writeByte(0);
			VarInt.write(buffer, 0);

			// write empty block data
			VarInt.write(buffer, 0);
		} else {
			// write min allowed bitsPerBlock
			final int bitsPerBlock = 4;
			buffer.writeByte(bitsPerBlock);

			// write palette with air entry
			VarInt.write(buffer, 1);
			VarInt.write(buffer, 0);

			// write empty block data
			int packetSize = BitStorage.getSerializedSize(bitsPerBlock, 4096);
			VarInt.write(buffer, packetSize);
			buffer.skipBytes(packetSize);
		}

		return buffer
				.capacity(buffer.readableBytes())
				.array();
	}

	private ChunkCapabilities() {
	}

	public static byte[] emptyBlockPalettedContainer() {
		return emptyBlockPalettedContainer;
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
