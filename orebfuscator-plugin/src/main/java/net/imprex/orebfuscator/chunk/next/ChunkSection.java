package net.imprex.orebfuscator.chunk.next;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.chunk.next.bitstorage.BitStorage;
import net.imprex.orebfuscator.chunk.next.palette.Palette;
import net.imprex.orebfuscator.chunk.next.strategies.BiomeStrategy;
import net.imprex.orebfuscator.chunk.next.strategies.BlockStrategy;

public class ChunkSection {

	static ByteBuf skip(ByteBuf buffer) {
		int startIndex = buffer.readerIndex();

		buffer.skipBytes(2); // skip block count

		int bitsPerBlock = buffer.readUnsignedByte();
		BlockStrategy.skipPalette(buffer, bitsPerBlock);
		BlockStrategy.skipBitStorage(buffer, bitsPerBlock);

		if (ChunkCapabilities.hasBiomePalettedContainer()) {
			int bitsPerBiome = buffer.readUnsignedByte();
			BiomeStrategy.skipPalette(buffer, bitsPerBiome);
			BiomeStrategy.skipBitStorage(buffer, bitsPerBiome);
		}

		int sectionLength = buffer.readerIndex() - startIndex;
		return buffer.slice(startIndex, sectionLength);
	}

	public final int blockCount;

	public final Palette blockPalette;
	public final BitStorage blockBitStorage;

	public final ByteBuf biomeSlice;
	public final ByteBuf sectionSlice;

	public ChunkSection(ByteBuf buffer) {
		int startIndex = buffer.readerIndex();

		this.blockCount = buffer.readShort();

		int bitsPerBlock = buffer.readUnsignedByte();
		this.blockPalette = BlockStrategy.readPalette(buffer, bitsPerBlock);
		this.blockBitStorage = BlockStrategy.readBitStorage(buffer, this.blockPalette);

		if (ChunkCapabilities.hasBiomePalettedContainer()) {
			int suffixStart = buffer.readerIndex();

			int bitsPerBiome = buffer.readUnsignedByte();
			BiomeStrategy.skipPalette(buffer, bitsPerBiome);
			BiomeStrategy.skipBitStorage(buffer, bitsPerBiome);

			int suffixLength = buffer.readerIndex() - suffixStart;
			this.biomeSlice = buffer.slice(suffixStart, suffixLength);
		} else {
			this.biomeSlice = Unpooled.EMPTY_BUFFER;
		}

		int sectionLength = buffer.readerIndex() - startIndex;
		this.sectionSlice = buffer.slice(startIndex, sectionLength);
	}

	public boolean isEmpty() {
		return this.blockCount == 0;
	}

	public int getBlockState(int x, int y, int z) {
		return getBlockState(BlockStrategy.getIndex(x, y, z));
	}

	public int getBlockState(int index) {
		return this.blockBitStorage.get(index);
	}
}
