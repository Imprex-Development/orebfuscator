package net.imprex.orebfuscator.chunk.next.strategies;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.next.bitstorage.BitStorage;
import net.imprex.orebfuscator.chunk.next.palette.Palette;

public class BlockStrategy {

	public static int size() {
		return PalettedContainerStrategy.BLOCK_STATES.size();
	}

	public static int getIndex(int x, int y, int z) {
		return PalettedContainerStrategy.BLOCK_STATES.getIndex(x, y, z);
	}

	public static void skipPalette(ByteBuf buffer, int bits) {
		PalettedContainerStrategy.BLOCK_STATES.getConfiguration(bits).skipBytes(buffer);
	}

	public static Palette readPalette(ByteBuf buffer, int bits) {
		return PalettedContainerStrategy.BLOCK_STATES.getConfiguration(bits).read(buffer);
	}

	public static Palette.Builder createPaletteBuilder() {
		return PalettedContainerStrategy.BLOCK_STATES.createBuilder();
	}

	public static void skipBitStorage(ByteBuf buffer, int bits) {
		BitStorage.skipBytes(buffer, bits);
	}

	public static BitStorage readBitStorage(ByteBuf buffer, Palette palette) {
		return BitStorage.read(buffer, PalettedContainerStrategy.BLOCK_STATES.size(), palette);
	}

	public static BitStorage.Writer createBitStorageWriter(ByteBuf buffer, Palette palette) {
		return BitStorage.createWriter(buffer, PalettedContainerStrategy.BLOCK_STATES.size(), palette);
	}
}
