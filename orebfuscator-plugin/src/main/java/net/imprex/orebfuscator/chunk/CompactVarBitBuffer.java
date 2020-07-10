package net.imprex.orebfuscator.chunk;

public class CompactVarBitBuffer implements VarBitBuffer {

	private final int bitsPerEntry;
	private final long adjustmentMask;

	private final int size;
	private final long[] buffer;

	public CompactVarBitBuffer(int bitsPerEntry, int size) {
		this.bitsPerEntry = bitsPerEntry;
		this.adjustmentMask = (1L << bitsPerEntry) - 1L;

		this.size = size;
		this.buffer = new long[(int) Math.ceil(bitsPerEntry * size / 64f)];
	}

	public int get(int index) {
		int position = index * this.bitsPerEntry;
		int startIndex = position >> 6;
		int endIndex = (position + this.bitsPerEntry - 1) >> 6;
		int bitOffset = position % 64;

		long value = this.buffer[startIndex] >>> bitOffset;
		if (startIndex != endIndex) {
			value |= this.buffer[endIndex] << (64 - bitOffset);
		}
		return (int) (value & this.adjustmentMask);
	}

	public void set(int index, int value) {
		int position = index * this.bitsPerEntry;
		int startIndex = position >> 6;
		int endIndex = (position + this.bitsPerEntry - 1) >> 6;
		int bitOffset = position % 64;

		this.buffer[startIndex] = this.buffer[startIndex] & ~(this.adjustmentMask << bitOffset) | (value & this.adjustmentMask) << bitOffset;

		if (startIndex != endIndex) {
			int endBitOffset = 64 - bitOffset;
			this.buffer[endIndex] = this.buffer[endIndex] & ~(this.adjustmentMask >> endBitOffset) | (value & this.adjustmentMask) >> endBitOffset;
		}
	}

	public long[] toArray() {
		return this.buffer;
	}

	public int size() {
		return this.size;
	}
}
