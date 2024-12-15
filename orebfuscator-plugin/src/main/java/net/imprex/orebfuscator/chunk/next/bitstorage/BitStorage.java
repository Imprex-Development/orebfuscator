package net.imprex.orebfuscator.chunk.next.bitstorage;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.chunk.next.palette.Palette;

public interface BitStorage {

	static int getSerializedSize(int bitsPerEntry, int size) {
		if (ChunkCapabilities.hasSingleValuePalette() && bitsPerEntry == 0) {
			return EmptyBitStorage.getSerializedSize();
		} else {
			return SimpleBitStorage.getSerializedSize(bitsPerEntry, size);
		}
	}

	static void skipBytes(ByteBuf buffer, int bitsPerEntry) {
		if (ChunkCapabilities.hasSingleValuePalette() && bitsPerEntry == 0) {
			EmptyBitStorage.skipBytes(buffer);
		} else {
			SimpleBitStorage.skipBytes(buffer);
		}
	}

	static BitStorage read(ByteBuf buffer, int size, Palette palette) {
		if (ChunkCapabilities.hasSingleValuePalette() && palette.getBitsPerEntry() == 0) {
			return EmptyBitStorage.read(buffer, size, palette);
		} else {
			return SimpleBitStorage.read(buffer, size, palette);
		}
	}

	static Writer createWriter(ByteBuf buffer, int size, Palette palette) {
		if (ChunkCapabilities.hasSingleValuePalette() && palette.getBitsPerEntry() == 0) {
			return EmptyBitStorage.createWriter(buffer);
		} else {
			return SimpleBitStorage.createWriter(buffer, size, palette);
		}
	}

	int get(int index);

	int size();

	interface Writer {

		default void throwIfNotExhausted() {
			if (!isExhausted()) {
				throw new IllegalStateException("BitStorage.Writer is not exhausted but closed!");
			}
		}

		boolean isExhausted();

		void write(int value);

	}
}
