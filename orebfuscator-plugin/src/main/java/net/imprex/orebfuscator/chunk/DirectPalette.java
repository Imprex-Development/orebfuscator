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
		if (ChunkCapabilities.hasDirectPaletteZeroLength()) {
			ByteBufUtil.readVarInt(buffer);
		}
	}

	@Override
	public void write(ByteBuf buffer) {
		if (ChunkCapabilities.hasDirectPaletteZeroLength()) {
			ByteBufUtil.writeVarInt(buffer, 0);
		}
	}
}
