package net.imprex.orebfuscator.chunk.next.palette;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.VarInt;

public class SingleValuedPalette implements Palette {

	public static final Factory FACTORY = new Factory() {
		
		@Override
		public void skipBytes(ByteBuf buffer) {
			VarInt.skipBytes(buffer);
		}
		
		@Override
		public Palette read(ByteBuf buffer, int bitsPerEntry) {
			int value = VarInt.read(buffer);
			return new SingleValuedPalette(value);
		}
		
		@Override
		public Palette create(int bitsPerEntry, List<Integer> values) {
			return new SingleValuedPalette(values.get(0));
		}
	};

	private final int value;

	public SingleValuedPalette(int value) {
		this.value = value;
	}

	@Override
	public int getBitsPerEntry() {
		return 0;
	}

	@Override
	public int getSerializedSize() {
		return VarInt.getSerializedSize(this.value);
	}

	@Override
	public int valueAtIndex(int index) {
		if (index != 0) {
			throw new IllegalArgumentException("Invalid index, single valued palette only has one entry!");
		}
		return this.value;
	}

	@Override
	public int indexForValue(int value) {
		if (value != this.value) {
			throw new IllegalArgumentException("Invalid value, single valued palette only has one entry!");
		}
		return 0;
	}

	@Override
	public void write(ByteBuf buffer) {
		VarInt.write(buffer, this.value);
	}
}
