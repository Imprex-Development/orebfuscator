package net.imprex.orebfuscator.chunk;

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.NmsInstance;

public class IndirectPalette implements Palette {

	private final byte[] blockToIndex;
	private final int[] indexToBlock;
	private int index = 0;

	private final ChunkSection chunkSection;
	private final int bitsPerBlock;

	public IndirectPalette(int bitsPerBlock, ChunkSection chunkSection) {
		this.bitsPerBlock = bitsPerBlock;
		this.chunkSection = chunkSection;

		this.blockToIndex = new byte[NmsInstance.get().getMaterialSize()];
		Arrays.fill(this.blockToIndex, (byte) -1);
		this.indexToBlock = new int[1 << bitsPerBlock];
	}

	@Override
	public int fromBlockId(int block) {
		int index = this.blockToIndex[block] & 0xFF;
		if (index == -1) {
			index = this.index++;

			if (index >= 1 << this.bitsPerBlock) {
				index = this.chunkSection.grow(this.bitsPerBlock + 1, block);
			} else {
				this.blockToIndex[block] = (byte) index;
				this.indexToBlock[index] = block;
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
		int size = ByteBufUtil.readVarInt(buffer);
		for (int i = 0; i < size; i++) {
			this.fromBlockId(ByteBufUtil.readVarInt(buffer));
		}
	}

	@Override
	public void write(ByteBuf buffer) {
		int size = this.indexToBlock.length;
		ByteBufUtil.writeVarInt(buffer, size);

		for (int i = 0; i < size; i++) {
			ByteBufUtil.writeVarInt(buffer, this.toBlockId(i));
		}
	}

	@Override
	public String toString() {
		return Arrays.toString(this.indexToBlock);
	}
}
