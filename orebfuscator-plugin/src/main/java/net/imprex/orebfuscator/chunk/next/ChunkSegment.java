package net.imprex.orebfuscator.chunk.next;

import io.netty.buffer.ByteBuf;

public interface ChunkSegment {

	boolean needsProcessing();

	boolean hasSection();

	default ByteBuf getBuffer() {
		throw new UnsupportedOperationException();
	}

	default ChunkSection getSection() {
		throw new UnsupportedOperationException();
	}

	public static class SkippedChunkSegment implements ChunkSegment {

		private final ByteBuf buffer;

		public SkippedChunkSegment(ByteBuf buffer) {
			this.buffer = buffer;
		}

		@Override
		public boolean needsProcessing() {
			return false;
		}

		@Override
		public boolean hasSection() {
			return false;
		}

		@Override
		public ByteBuf getBuffer() {
			return this.buffer;
		}
	}

	public static class ReadOnlyChunkSegment implements ChunkSegment {

		private final ChunkSection section;

		public ReadOnlyChunkSegment(ChunkSection section) {
			this.section = section;
		}

		@Override
		public boolean needsProcessing() {
			return false;
		}

		@Override
		public boolean hasSection() {
			return true;
		}

		@Override
		public ByteBuf getBuffer() {
			return this.section.sectionSlice;
		}

		@Override
		public ChunkSection getSection() {
			return this.section;
		}
	}

	public static class ProcessingChunkSegment implements ChunkSegment {

		private final ChunkSection section;

		public ProcessingChunkSegment(ChunkSection section) {
			this.section = section;
		}

		@Override
		public boolean needsProcessing() {
			return true;
		}

		@Override
		public boolean hasSection() {
			return true;
		}

		@Override
		public ChunkSection getSection() {
			return this.section;
		}
	}
}
