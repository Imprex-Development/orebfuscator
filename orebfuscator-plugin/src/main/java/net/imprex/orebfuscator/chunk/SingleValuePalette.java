package net.imprex.orebfuscator.chunk;

import io.netty.buffer.ByteBuf;

public class SingleValuePalette implements Palette {

	private final ChunkSection chunkSection;

	private int value = -1;

	public SingleValuePalette(ChunkSection chunkSection, int value) {
		this.chunkSection = chunkSection;
		this.value = value;
	}

	@Override
	public int idFor(int value) {
		if (this.value != -1 && value != this.value) {
			return this.chunkSection.grow(1, value);
		} else {
			this.value = value;
			return 0;
		}
	}

	@Override
	public int valueFor(int id) {
		if (this.value != -1 && id == 0) {
			return this.value;
		} else {
			throw new IllegalStateException("value isn't initialized");
		}
	}

	@Override
	public void read(ByteBuf buffer) {
		this.value = VarInt.read(buffer);
	}

	@Override
	public void write(ByteBuf buffer) {
		if (this.value == -1) {
			throw new IllegalStateException("value isn't initialized");
		} else {
			VarInt.write(buffer, this.value);
		}
	}
}
