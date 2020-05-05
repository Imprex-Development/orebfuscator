package net.imprex.orebfuscator.chunk;

import io.netty.buffer.ByteBuf;

public class ByteBufUtil {

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
