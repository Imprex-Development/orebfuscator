package net.imprex.orebfuscator.chunk;

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.NmsInstance;

public class IndirectPalette implements Palette {

	private final byte[] blockToIndex;
	private final int[] indexToBlock;
	private int size = 0;

	private final ChunkSection chunkSection;
	private final int bitsPerBlock;

	public IndirectPalette(int bitsPerBlock, ChunkSection chunkSection) {
		this.bitsPerBlock = bitsPerBlock;
		this.chunkSection = chunkSection;

		// TODO improve block to index
		this.blockToIndex = new byte[NmsInstance.getTotalBlockCount()];
		Arrays.fill(this.blockToIndex, (byte) 0xFF);
		this.indexToBlock = new int[1 << bitsPerBlock];
	}

	@Override
	public int fromBlockId(int block) {
		int index = this.blockToIndex[block] & 0xFF;
		if (index == 0xFF) {
			index = this.size++;

			if (index != 0xFF && index < this.indexToBlock.length) {
				this.blockToIndex[block] = (byte) index;
				this.indexToBlock[index] = block;
			} else {
				index = this.chunkSection.grow(this.bitsPerBlock + 1, block);
			}
		}

		return index;
	}

	@Override
	public int toBlockId(int index) {
		return this.indexToBlock[index];
	}

	@Override
	public void read(ByteBuf buffer) {
		this.size = ByteBufUtil.readVarInt(buffer);
		for (int i = 0; i < size; i++) {
			int blockId = ByteBufUtil.readVarInt(buffer);
			this.indexToBlock[i] = blockId;
			this.blockToIndex[blockId] = (byte) i;
		}
	}

	@Override
	public void write(ByteBuf buffer) {
		ByteBufUtil.writeVarInt(buffer, this.size);

		for (int i = 0; i < this.size; i++) {
			ByteBufUtil.writeVarInt(buffer, this.toBlockId(i));
		}
	}
}
