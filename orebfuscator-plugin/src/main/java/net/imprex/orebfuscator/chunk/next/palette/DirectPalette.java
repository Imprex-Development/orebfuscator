package net.imprex.orebfuscator.chunk.next.palette;

import java.util.List;

import io.netty.buffer.ByteBuf;

public class DirectPalette implements Palette {

	public static final Factory createFactory(int maxBitsPerEntry) {
		DirectPalette palette = new DirectPalette(maxBitsPerEntry);

		return new Factory() {
			
			@Override
			public void skipBytes(ByteBuf buffer) {
			}
			
			@Override
			public Palette read(ByteBuf buffer, int bitsPerEntry) {
				return palette;
			}
			
			@Override
			public Palette create(int bitsPerEntry, List<Integer> values) {
				return palette;
			}
		};
	}

	private final int bitsPerEntry;

	public DirectPalette(int maxBitsPerEntry) {
		this.bitsPerEntry = maxBitsPerEntry;
	}

	@Override
	public int getBitsPerEntry() {
		return this.bitsPerEntry;
	}

	@Override
	public int valueAtIndex(int index) {
		return index;
	}

	@Override
	public int indexForValue(int value) {
		return value;
	}

	@Override
	public void write(ByteBuf buffer) {
	}

	@Override
	public int getSerializedSize() {
		return 0;
	}
}
