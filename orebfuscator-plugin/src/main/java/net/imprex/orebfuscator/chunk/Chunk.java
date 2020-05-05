package net.imprex.orebfuscator.chunk;

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

public class Chunk implements AutoCloseable {

	public static Chunk fromChunkStruct(ChunkStruct chunkStruct) {
		int sectionCount;
		for (sectionCount = 0; (chunkStruct.primaryBitMask & (1 << sectionCount)) != 0; sectionCount++) {
		}

		int extraBytes = ChunkCapabilities.hasLightArray ? 2048 : 0;
		if (chunkStruct.isOverworld) {
			extraBytes *= 2;
		}

		return new Chunk(chunkStruct, sectionCount, extraBytes);
	}

	private final int chunkX;
	private final int chunkZ;

	private final int sectionCount;
	private final int extraBytes;

	private final ChunkSectionHolder[] sections = new ChunkSectionHolder[16];

	private final ByteBuf inputBuffer;
	private final ByteBuf outputBuffer;

	private Chunk(ChunkStruct chunkStruct, int sectionCount, int extraBytes) {
		this.chunkX = chunkStruct.chunkX;
		this.chunkZ = chunkStruct.chunkZ;

		this.sectionCount = sectionCount;
		this.extraBytes = extraBytes;

		this.inputBuffer = Unpooled.wrappedBuffer(chunkStruct.data);
		this.outputBuffer = PooledByteBufAllocator.DEFAULT.heapBuffer(chunkStruct.data.length);

		for (int sectionIndex = 0; sectionIndex < this.sectionCount; sectionIndex++) {
			this.sections[sectionIndex] = new ChunkSectionHolder();
		}
	}

	public int getSectionCount() {
		return sectionCount;
	}

	public ChunkSection getSection(int index) {
		return this.sections[index].chunkSection;
	}

	public int getBlock(int x, int y, int z) {
		int chunkY = y >> 4;
		if (x >> 4 == this.chunkX && z >> 4 == this.chunkZ && chunkY < this.sectionCount) {
			return this.sections[chunkY].data[ChunkSection.positionToIndex(x & 0xF, y & 0xF, z & 0xF)];
		}

		return -1;
	}

	public byte[] finalizeOutput() {
		for (int sectionIndex = 0; sectionIndex < this.sectionCount; sectionIndex++) {
			this.sections[sectionIndex].write();
		}
		this.outputBuffer.writeBytes(this.inputBuffer);
		return Arrays.copyOfRange(this.outputBuffer.array(), this.outputBuffer.arrayOffset(),
				this.outputBuffer.arrayOffset() + this.outputBuffer.readableBytes());
	}

	@Override
	public void close() throws Exception {
		this.inputBuffer.release();
		this.outputBuffer.release();
	}

	private class ChunkSectionHolder {

		public final ChunkSection chunkSection;
		public final int[] data;
		public final int offset;

		public ChunkSectionHolder() {
			this.chunkSection = new ChunkSection();
			this.data = this.chunkSection.read(inputBuffer);
			this.offset = inputBuffer.readerIndex();
			inputBuffer.skipBytes(extraBytes);
		}

		public void write() {
			this.chunkSection.write(outputBuffer);
			outputBuffer.writeBytes(inputBuffer, this.offset, extraBytes);
		}
	}
}
