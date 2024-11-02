package net.imprex.orebfuscator.chunk.next.palette;

import java.util.BitSet;
import java.util.List;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.VarInt;

public class IndirectPalette implements Palette {

	public static final Factory FACTORY = new Factory() {
		
		@Override
		public void skipBytes(ByteBuf buffer) {
			int size = VarInt.read(buffer);
			for (int i = 0; i < size; i++) {
				VarInt.skipBytes(buffer);
			}
		}
		
		@Override
		public Palette read(ByteBuf buffer, int bitsPerEntry) {
			int size = VarInt.read(buffer);
			if (size > (1 << bitsPerEntry)) {
				throw new IllegalStateException(
						String.format("indirect palette is too big (got=%d < expected=%d)", size, (1 << bitsPerEntry)));
			}

			int[] byIndex = new int[size];
			int maxValue = 0;

			for (int index = 0; index < size; index++) {
				int value = VarInt.read(buffer);
				byIndex[index] = value;

				if (value > maxValue) {
					maxValue = value;
				}
			}

			return new IndirectPalette(bitsPerEntry, byIndex, maxValue + 1);
		}
		
		@Override
		public Palette create(int bitsPerEntry, List<Integer> values) {
			int[] byIndex = new int[values.size()];
			int maxValue = 0;

			for (int index = 0; index < values.size(); index++) {
				int value = values.get(index);
				byIndex[index] = value;

				if (value > maxValue) {
					maxValue = value;
				}
			}

			return new IndirectPalette(bitsPerEntry, byIndex, maxValue + 1);
		}
	};

	private final int bitsPerEntry;
	private final int size;

	private final BitSet byValuePresent;
	private final byte[] byValue;
	private final int[] byIndex;
	
	/*
	 * TODO: remove byValue if PaletteBuilder is reworked
	 */

	public IndirectPalette(int bitsPerEntry, int[] byIndex, int maxValue) {
		this.size = byIndex.length;
		this.bitsPerEntry = bitsPerEntry;

		this.byValuePresent = new BitSet(maxValue);
		this.byValue = new byte[maxValue];

		this.byIndex = byIndex;

		for (int index = 0; index < byIndex.length; index++) {
			int value = byIndex[index];
			this.byValue[value] = (byte) index;
			this.byValuePresent.set(value);
		}
	}

	@Override
	public int getBitsPerEntry() {
		return this.bitsPerEntry;
	}

	@Override
	public int getSerializedSize() {
		int serializedSize = VarInt.getSerializedSize(this.size);

		for (int index = 0; index < this.size; index++) {
			serializedSize += VarInt.getSerializedSize(this.byIndex[index]);
		}

		return serializedSize;
	}

	@Override
	public int valueAtIndex(int index) {
		if (index < 0 || index >= this.byIndex.length) {
			throw new IndexOutOfBoundsException();
		} else {
			return this.byIndex[index];
		}
	}

	@Override
	public int indexForValue(int value) {
		if (!this.byValuePresent.get(value)) {
			throw new IllegalArgumentException("value=" + value);
		}
		return this.byValue[value] & 0xFF;
	}

	@Override
	public void write(ByteBuf buffer) {
		VarInt.write(buffer, this.size);

		for (int index = 0; index < this.size; index++) {
			VarInt.write(buffer, this.byIndex[index]);
		}
	}
}
