package net.imprex.orebfuscator.chunk;

import io.netty.buffer.ByteBuf;

public class DirectPalette implements Palette {

	@Override
	public int fromBlockId(int blockId) {
		return blockId;
	}

	@Override
	public int toBlockId(int id) {
		return id;
	}

	@Override
	public void read(ByteBuf buffer) {
		if (ChunkCapabilities.hasDirectPaletteZeroLength) {
			ByteBufUtil.readVarInt(buffer);
		}
	}

	@Override
	public void write(ByteBuf buffer) {
		if (ChunkCapabilities.hasDirectPaletteZeroLength) {
			ByteBufUtil.writeVarInt(buffer, 0);
		}
	}
}
