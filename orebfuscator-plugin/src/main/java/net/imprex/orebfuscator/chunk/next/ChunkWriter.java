package net.imprex.orebfuscator.chunk.next;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.chunk.next.bitstorage.BitStorage;
import net.imprex.orebfuscator.chunk.next.palette.Palette;
import net.imprex.orebfuscator.chunk.next.strategies.BlockStrategy;

public class ChunkWriter {

	private static int getSerializedSize(Palette palette, int suffixLength) {
		return 3 // blockCount + bitsPerBlock
				+ palette.getSerializedSize()
				+ BitStorage.getSerializedSize(palette.getBitsPerEntry(), BlockStrategy.size())
				+ suffixLength;
	}

	private final ByteBuf outputBuffer;
	private final ChunkSegment[] sections;

	private int nextSectionIndex = 0;

	public ChunkWriter(ByteBuf outputBuffer, ChunkSegment[] sections) {
		this.outputBuffer = outputBuffer;
		this.sections = sections;
	}

	public boolean hasNext() {
		return this.nextSectionIndex < this.sections.length;
	}

	public int getSectionIndex() {
		return this.nextSectionIndex - 1;
	}

	public ChunkSection getOrWriteNext() {
		ChunkSegment sectionHolder = this.sections[this.nextSectionIndex++];
		// skip non-present chunks (e.g. heightMask)
		if (sectionHolder == null) {
			return null;
		}

		// write section buffer if present and skip processing
		if (!sectionHolder.needsProcessing()) {
			this.outputBuffer.writeBytes(sectionHolder.getBuffer());
			return null;
		}

		// always write empty sections without processing
		ChunkSection chunkSection = sectionHolder.getSection();
		if (chunkSection != null && chunkSection.isEmpty()) {
			this.writeEmpty(chunkSection.biomeSlice);
			return null;
		}

		return chunkSection;
	}

	public void writeSection(Palette palette, int blockCount, int[] blockStates, ByteBuf suffix) {
		// skip write if block count is zero
		if (blockCount == 0) {
			writeEmpty(suffix);
			return;
		}

		// make sure buffer has enough space
		int serializedSize = getSerializedSize(palette, suffix.readableBytes());
		this.outputBuffer.ensureWritable(serializedSize);

		// write block count
		this.outputBuffer.writeShort(blockCount);

		// write bitsPerBlock + palette
		this.outputBuffer.writeByte(palette.getBitsPerEntry());
		palette.write(this.outputBuffer);

		// write block data
		BitStorage.Writer blockWriter = BlockStrategy.createBitStorageWriter(this.outputBuffer, palette);
		if (palette.getBitsPerEntry() > 0) {
			for (int i = 0; i < blockStates.length; i++) {
				blockWriter.write(blockStates[i]);
			}
			blockWriter.throwIfNotExhausted();
		}

		// append remaining suffix
		this.outputBuffer.writeBytes(suffix);
	}

	private void writeEmpty(ByteBuf suffix) {
		// write default empty section
		this.outputBuffer.writeBytes(ChunkCapabilities.emptyBlockPalettedContainer());

		// append remaining suffix
		this.outputBuffer.writeBytes(suffix);
	}
}
