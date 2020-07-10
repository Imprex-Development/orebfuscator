package net.imprex.orebfuscator.chunk;

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

public class Chunk implements AutoCloseable {

	public static Chunk fromChunkStruct(ChunkStruct chunkStruct) {
		int extraBytes = ChunkCapabilities.hasLightArray ? 2048 : 0;
		if (chunkStruct.isOverworld) {
			extraBytes *= 2;
		}

		return new Chunk(chunkStruct, extraBytes);
	}

	private final int chunkX;
	private final int chunkZ;

	private final int extraBytes;

	private final ChunkSectionHolder[] sections = new ChunkSectionHolder[16];

	private final ByteBuf inputBuffer;
	private final ByteBuf outputBuffer;

	private Chunk(ChunkStruct chunkStruct, int extraBytes) {
		this.chunkX = chunkStruct.chunkX;
		this.chunkZ = chunkStruct.chunkZ;

		this.extraBytes = extraBytes;

		this.inputBuffer = Unpooled.wrappedBuffer(chunkStruct.data);
		this.outputBuffer = PooledByteBufAllocator.DEFAULT.heapBuffer(chunkStruct.data.length);

		for (int sectionIndex = 0; sectionIndex < 16; sectionIndex++) {
			if ((chunkStruct.primaryBitMask & (1 << sectionIndex)) != 0) {
				this.sections[sectionIndex] = new ChunkSectionHolder();
			}
		}
	}

	public int getSectionCount() {
		return this.sections.length;
	}

	public ChunkSection getSection(int index) {
		ChunkSectionHolder chunkSection = this.sections[index];
		if (chunkSection != null) {
			return chunkSection.chunkSection;
		}
		return null;
	}

	public int getBlock(int x, int y, int z) {
		int chunkY = y >> 4;
		if (x >> 4 == this.chunkX && z >> 4 == this.chunkZ) {
			ChunkSectionHolder chunkSection = this.sections[chunkY];
			if (chunkSection != null) {
				return chunkSection.data[ChunkSection.positionToIndex(x & 0xF, y & 0xF, z & 0xF)];
			}
		}

		return -1;
	}

	public byte[] finalizeOutput() {
		for (ChunkSectionHolder chunkSection : this.sections) {
			if (chunkSection != null) {
				chunkSection.write();
			}
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
