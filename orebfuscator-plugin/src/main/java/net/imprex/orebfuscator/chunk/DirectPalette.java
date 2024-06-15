package net.imprex.orebfuscator.chunk;

import io.netty.buffer.ByteBuf;

public class DirectPalette implements Palette {

	@Override
	public int idFor(int value) {
		return value;
	}

	@Override
	public int valueFor(int id) {
		return id;
	}

	@Override
	public void read(ByteBuf buffer) {
	}

	@Override
	public void write(ByteBuf buffer) {
	}
}
