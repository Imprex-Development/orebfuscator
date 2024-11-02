package net.imprex.orebfuscator.chunk.bitstorage;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.imprex.orebfuscator.chunk.VarInt;
import net.imprex.orebfuscator.chunk.next.bitstorage.BitStorage;
import net.imprex.orebfuscator.chunk.next.bitstorage.SimpleBitStorage;
import net.imprex.orebfuscator.chunk.next.palette.Palette;

public class SimpleBitStorageTest {

	private static final byte[] IDENTITY_BUFFER = readIdentityBuffer("src/test/resources/simple-bit-storage.bin");
	private static final int TEST_ENTRY_COUNT = 65535;

	private static byte[] readIdentityBuffer(String path) {
		try {
			return Files.readAllBytes(Paths.get(path));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testReadWriteSkip() {
		for (int bits = 1; bits < 32; bits++) {
			Palette palette = new DummyIdentityPalette(bits);
			int maxValue = (1 << bits) - 1;

			int serializedSize = SimpleBitStorage.getSerializedSize(bits, TEST_ENTRY_COUNT);
			ByteBuf buffer = Unpooled.buffer(serializedSize);

			BitStorage.Writer writer = SimpleBitStorage.createWriter(buffer, TEST_ENTRY_COUNT, palette);

			// initial writer state check
			assertEquals(false, writer.isExhausted());
			assertThrows(IllegalStateException.class, () -> writer.throwIfNotExhausted());

			for (int i = 0; i < TEST_ENTRY_COUNT; i++) {
				assertEquals(false, writer.isExhausted());
				writer.write(i % maxValue);
			}

			// exhausted writer state check
			assertEquals(true, writer.isExhausted());
			assertDoesNotThrow(() -> writer.throwIfNotExhausted());
			assertThrows(IllegalStateException.class, () -> writer.write(0));

			// expected vs actual serialized size check
			assertEquals(serializedSize, buffer.readableBytes());

			BitStorage bitStorage = SimpleBitStorage.read(buffer, TEST_ENTRY_COUNT, palette);
			for (int i = 0; i < TEST_ENTRY_COUNT; i++) {
				assertEquals(i % maxValue, bitStorage.get(i));
			}

			assertEquals(0, buffer.readableBytes());
			assertEquals(TEST_ENTRY_COUNT, bitStorage.size());

			buffer.readerIndex(0);
			SimpleBitStorage.skipBytes(buffer);

			assertEquals(0, buffer.readableBytes());
		}
	}

	@Test
	public void testFaweFix() {
		Palette palette = new DummyIdentityPalette(4);

		int faweLength = (int) Math.ceil(TEST_ENTRY_COUNT / 64f);
		int fawePacketSize = faweLength * Long.BYTES;
		ByteBuf buffer = Unpooled.buffer(VarInt.getSerializedSize(faweLength) + fawePacketSize);

		VarInt.write(buffer, faweLength);
		buffer.writerIndex(buffer.writerIndex() + fawePacketSize);

		BitStorage bitStorage = SimpleBitStorage.read(buffer, TEST_ENTRY_COUNT, palette);
		for (int i = 0; i < TEST_ENTRY_COUNT; i++) {
			assertEquals(0, bitStorage.get(i));
		}

		assertEquals(0, buffer.readableBytes());
	}

	@Test
	public void testReadIdentity() {
		Palette palette = new DummyIdentityPalette(16);
		ByteBuf buffer = Unpooled.wrappedBuffer(IDENTITY_BUFFER).asReadOnly();

		BitStorage bitStorage = SimpleBitStorage.read(buffer, TEST_ENTRY_COUNT, palette);
		for (int i = 0; i < TEST_ENTRY_COUNT; i++) {
			assertEquals(i, bitStorage.get(i));
		}
	}

	public void testWriteIdentity() {
		Palette palette = new DummyIdentityPalette(16);
		byte[] output = new byte[IDENTITY_BUFFER.length];

		ByteBuf buffer = Unpooled.wrappedBuffer(output);
		buffer.writerIndex(0);

		BitStorage.Writer writer = new SimpleBitStorage.Writer(buffer, TEST_ENTRY_COUNT, palette);
		for (int i = 0; i < TEST_ENTRY_COUNT; i++) {
			writer.write(i);
		}

		assertArrayEquals(IDENTITY_BUFFER, output);
	}

	public class DummyIdentityPalette implements Palette {

		private final int bitsPerEntry;

		public DummyIdentityPalette(int bitsPerEntry) {
			this.bitsPerEntry = bitsPerEntry;
		}

		@Override
		public int getBitsPerEntry() {
			return this.bitsPerEntry;
		}

		@Override
		public int getSerializedSize() {
			throw new UnsupportedOperationException();
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
			throw new UnsupportedOperationException();
		}
	}
}
