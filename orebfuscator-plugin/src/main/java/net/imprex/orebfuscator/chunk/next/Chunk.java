package net.imprex.orebfuscator.chunk.next;

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.chunk.next.ChunkSegment.ProcessingChunkSegment;
import net.imprex.orebfuscator.chunk.next.ChunkSegment.ReadOnlyChunkSegment;
import net.imprex.orebfuscator.chunk.next.ChunkSegment.SkippedChunkSegment;
import net.imprex.orebfuscator.config.WorldConfigBundle;
import net.imprex.orebfuscator.util.HeightAccessor;

public class Chunk implements AutoCloseable {

	public static Chunk fromChunkStruct(ChunkStruct chunkStruct, WorldConfigBundle bundle) {
		return new Chunk(chunkStruct, bundle);
	}

	private final int chunkX;
	private final int chunkZ;

	private final HeightAccessor heightAccessor;
	private final ChunkSegment[] sections;

	private final ByteBuf inputBuffer;
	private final ByteBuf outputBuffer;

	private final ChunkWriter writer;

	private Chunk(ChunkStruct chunkStruct, WorldConfigBundle bundle) {
		this.chunkX = chunkStruct.chunkX;
		this.chunkZ = chunkStruct.chunkZ;

		this.heightAccessor = HeightAccessor.get(chunkStruct.world);
		this.sections = new ChunkSegment[this.heightAccessor.getSectionCount()];

		this.inputBuffer = Unpooled.wrappedBuffer(chunkStruct.data);
		this.outputBuffer = PooledByteBufAllocator.DEFAULT.heapBuffer(chunkStruct.data.length);

		for (int sectionIndex = 0; sectionIndex < this.sections.length; sectionIndex++) {
			try {
				if (chunkStruct.sectionMask.get(sectionIndex)) {
					ChunkSegment sectionHolder;

					if (bundle.skipReadSectionIndex(sectionIndex)) {
						sectionHolder = new SkippedChunkSegment(ChunkSection.skip(inputBuffer));
					} else {
						ChunkSection chunkSection = new ChunkSection(inputBuffer);
						if (bundle.skipProcessingSectionIndex(sectionIndex)) {
							sectionHolder = new ReadOnlyChunkSegment(chunkSection);
						} else {
							sectionHolder = new ProcessingChunkSegment(chunkSection);
						}
					}

					this.sections[sectionIndex] = sectionHolder;
				}
			} catch (Exception e) {
				throw new RuntimeException("An error occurred while reading chunk section: " + sectionIndex, e);
			}
		}

		this.writer = new ChunkWriter(this.outputBuffer, this.sections);
	}

	public ChunkWriter getWriter() {
		return writer;
	}

	public HeightAccessor getHeightAccessor() {
		return heightAccessor;
	}

	public int getBlockState(int x, int y, int z) {
		if (x >> 4 == this.chunkX && z >> 4 == this.chunkZ) {
			ChunkSegment segment = this.sections[this.heightAccessor.getSectionIndex(y)];
			if (segment != null && segment.hasSection()) {
				return segment.getSection().getBlockState(x, y, z);
			}
			return 0;
		}
		return -1;
	}

	public byte[] finalizeOutput() {
		this.outputBuffer.writeBytes(this.inputBuffer);
		return Arrays.copyOfRange(this.outputBuffer.array(), this.outputBuffer.arrayOffset(),
				this.outputBuffer.arrayOffset() + this.outputBuffer.readableBytes());
	}

	@Override
	public void close() throws Exception {
		this.inputBuffer.release();
		this.outputBuffer.release();
	}
}
