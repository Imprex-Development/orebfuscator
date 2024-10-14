package net.imprex.orebfuscator.chunk.next.palette;

import java.util.List;

import io.netty.buffer.ByteBuf;

public interface Palette {

	int getBitsPerEntry();

	int getSerializedSize();

	int valueAtIndex(int index);

	int indexForValue(int value);

	void write(ByteBuf buffer);

	public interface Factory {

		void skipBytes(ByteBuf buffer);

		Palette read(ByteBuf buffer, int bitsPerEntry);

		Palette create(int bitsPerEntry, List<Integer> values);
	}

	public interface Builder {

		Builder add(int value);

		Palette build();
	}
}
