package net.imprex.orebfuscator.chunk.bitstorage;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.imprex.orebfuscator.chunk.next.bitstorage.BitStorage;
import net.imprex.orebfuscator.chunk.next.bitstorage.EmptyBitStorage;
import net.imprex.orebfuscator.chunk.next.palette.IndirectPalette;
import net.imprex.orebfuscator.chunk.next.palette.Palette;
import net.imprex.orebfuscator.chunk.next.palette.SingleValuedPalette;

public class EmptyBitStorageTest {

	private static final int STORAGE_SIZE = 4095;
	private static final int VALUE = 7;

	@Test
	public void testReadWriteSkip() {
		Palette validPalette = new SingleValuedPalette(VALUE);
		Palette invalidPalette = new IndirectPalette(1, new int[0], 0);

		int serializedSize = EmptyBitStorage.getSerializedSize();
		ByteBuf buffer = Unpooled.buffer(serializedSize);

		BitStorage.Writer writer = EmptyBitStorage.createWriter(buffer);

		assertEquals(true, writer.isExhausted());
		assertDoesNotThrow(() -> writer.throwIfNotExhausted());

		assertThrows(UnsupportedOperationException.class, () -> writer.write(0));
		assertEquals(serializedSize, buffer.readableBytes());

		assertThrows(IllegalArgumentException.class, () -> EmptyBitStorage.read(buffer, STORAGE_SIZE, invalidPalette));
		BitStorage bitStorage = EmptyBitStorage.read(buffer, STORAGE_SIZE, validPalette);

		for (int i = 0; i < STORAGE_SIZE; i++) {
			assertEquals(VALUE, bitStorage.get(i));
		}

		assertEquals(0, buffer.readableBytes());
		assertEquals(STORAGE_SIZE, bitStorage.size());

		buffer.readerIndex(0);

		EmptyBitStorage.skipBytes(buffer);

		assertEquals(0, buffer.readableBytes());
	}
}