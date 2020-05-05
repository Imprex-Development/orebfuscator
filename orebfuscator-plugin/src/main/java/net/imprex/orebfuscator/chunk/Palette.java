package net.imprex.orebfuscator.chunk;

import io.netty.buffer.ByteBuf;

public interface Palette {

	int fromBlockId(int blockId);

	int toBlockId(int id);

	void read(ByteBuf buffer);

	void write(ByteBuf buffer);

}
