package net.imprex.orebfuscator.chunk;

import io.netty.buffer.ByteBuf;

public class ByteBufUtil {

	public static int getVarIntSize(int value) {
		for (int bytes = 1; bytes < 5; bytes++) {
			if ((value & -1 << bytes * 7) == 0) {
				return bytes;
			}
		}
		return 5;
	}

	public static void skipVarInt(ByteBuf buffer) {
		int bytes = 0;
		byte in;
		do {
			in = buffer.readByte();
			if (++bytes > 5) {
				throw new IndexOutOfBoundsException("varint32 too long");
			}
		} while ((in & 0x80) != 0);
	}

	public static int readVarInt(ByteBuf buffer) {
		int out = 0;
		int bytes = 0;
		byte in;
		do {
			in = buffer.readByte();
			out |= (in & 0x7F) << bytes++ * 7;
			if (bytes > 5) {
				throw new IndexOutOfBoundsException("varint32 too long");
			}
		} while ((in & 0x80) != 0);
		return out;
	}

	public static void writeVarInt(ByteBuf buffer, int value) {
		while ((value & -0x80) != 0) {
			buffer.writeByte(value & 0x7F | 0x80);
			value >>>= 7;
		}
		buffer.writeByte(value);
	}
}
