package net.imprex.orebfuscator.chunk.next.bitstorage;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.VarInt;
import net.imprex.orebfuscator.chunk.next.palette.Palette;

public class EmptyBitStorage implements BitStorage {

	public static int getSerializedSize() {
		return 1;
	}

	public static void skipBytes(ByteBuf buffer) {
		VarInt.skipBytes(buffer);
	}

	public static BitStorage read(ByteBuf buffer, int size, Palette palette) {
		if (palette.getBitsPerEntry() != 0) {
			throw new IllegalArgumentException("Invalid palette, expected single valued palette!");
		}

		// skip length field
		VarInt.skipBytes(buffer);

		return new EmptyBitStorage(size, palette.valueAtIndex(0));
	}

	public static Writer createWriter(ByteBuf buffer) {
		VarInt.write(buffer, 0);
		return Writer.INSTANCE;
	}

	private final int size;
	private final int value;

	public EmptyBitStorage(int size, int value) {
		this.size = size;
		this.value = value;
	}

	@Override
	public int get(int index) {
		return this.value;
	}

	@Override
	public int size() {
		return this.size;
	}

	private static class Writer implements BitStorage.Writer {

		private static final Writer INSTANCE = new Writer();

		private Writer() {
		}

		@Override
		public boolean isExhausted() {
			return true;
		}

		@Override
		public void write(int value) {
			throw new UnsupportedOperationException("Can't write to EmptyBitStorage!");
		}
	}
}
