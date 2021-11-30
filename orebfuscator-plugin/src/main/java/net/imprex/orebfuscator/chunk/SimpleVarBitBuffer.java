package net.imprex.orebfuscator.chunk;

public class SimpleVarBitBuffer implements VarBitBuffer {

	public static int calculateArraySize(int bitsPerEntry, int size) {
		return bitsPerEntry == 0 ? 0 : (int) Math.ceil((float) size / (64 / bitsPerEntry));
	}

	private final int bitsPerEntry;
	private final int entriesPerLong;
	private final long adjustmentMask;

	private final int size;
	private final long[] buffer;

	public SimpleVarBitBuffer(int bitsPerEntry, int size) {
		this.bitsPerEntry = bitsPerEntry;
		this.entriesPerLong = 64 / bitsPerEntry;
		this.adjustmentMask = (1L << bitsPerEntry) - 1L;

		this.size = size;
		this.buffer = new long[(int) Math.ceil((float) size / this.entriesPerLong)];
	}

	public int get(int index) {
		int position = index / this.entriesPerLong;
		int offset = (index - position * this.entriesPerLong) * this.bitsPerEntry;
		return (int) (this.buffer[position] >> offset & this.adjustmentMask);
	}

	public void set(int index, int value) {
		int position = index / this.entriesPerLong;
		int offset = (index - position * this.entriesPerLong) * this.bitsPerEntry;
		this.buffer[position] = this.buffer[position] & ~(this.adjustmentMask << offset)
				| (value & this.adjustmentMask) << offset;
	}

	public long[] toArray() {
		return this.buffer;
	}

	public int size() {
		return this.size;
	}

	@Override
	public String toString() {
		return String.format("[size=%d, length=%d, bitsPerEntry=%d, entriesPerLong=%d]", size, buffer.length, bitsPerEntry, entriesPerLong);
	}
}
