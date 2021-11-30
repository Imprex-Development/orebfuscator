package net.imprex.orebfuscator.chunk;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.NmsInstance;

public class ChunkSection {

	private int blockCount;
	private int bitsPerBlock = -1;

	private Palette palette;
	private VarBitBuffer data;

	public ChunkSection() {
		this.setBitsPerBlock(0, true);
	}

	private void setBitsPerBlock(int bitsPerBlock, boolean grow) {
		if (this.bitsPerBlock != bitsPerBlock) {
			if (ChunkCapabilities.hasSingleValuePalette() && bitsPerBlock == 0) {
				this.bitsPerBlock = 0;
				this.palette = new SingleValuePalette(this, 0);
			} else if (!grow && bitsPerBlock == 1) {
				// fix: fawe chunk format incompatibility with bitsPerBlock == 1
				// https://github.com/Imprex-Development/Orebfuscator/issues/36
				this.bitsPerBlock = bitsPerBlock;
				this.palette = new IndirectPalette(this.bitsPerBlock, this);
			} else if (bitsPerBlock <= 8) {
				this.bitsPerBlock = Math.max(4, bitsPerBlock);
				this.palette = new IndirectPalette(this.bitsPerBlock, this);
			} else {
				this.bitsPerBlock = NmsInstance.getBitsPerBlock();
				this.palette = new DirectPalette();
			}

			if (this.bitsPerBlock == 0) {
				this.data = new ZeroVarBitBuffer(4096);
			} else {
				this.data = ChunkCapabilities.createVarBitBuffer(this.bitsPerBlock, 4096);
			}
		}
	}

	int grow(int bitsPerBlock, int blockId) {
		Palette palette = this.palette;
		VarBitBuffer data = this.data;

		this.setBitsPerBlock(bitsPerBlock, true);

		for (int i = 0; i < data.size(); i++) {
			int preBlockId = palette.valueFor(data.get(i));
			this.data.set(i, this.palette.idFor(preBlockId));
		}

		return this.palette.idFor(blockId);
	}

	static int positionToIndex(int x, int y, int z) {
		return y << 8 | z << 4 | x;
	}

	public void setBlock(int x, int y, int z, int blockId) {
		this.setBlock(positionToIndex(x, y, z), blockId);
	}

	public void setBlock(int index, int blockId) {
		int prevBlockId = this.getBlock(index);

		if (!NmsInstance.isAir(prevBlockId)) {
			--this.blockCount;
		}

		if (!NmsInstance.isAir(blockId)) {
			++this.blockCount;
		}

		int paletteIndex = this.palette.idFor(blockId);
		this.data.set(index, paletteIndex);
	}

	public int getBlock(int x, int y, int z) {
		return this.getBlock(positionToIndex(x, y, z));
	}

	public int getBlock(int index) {
		return this.palette.valueFor(this.data.get(index));
	}

	public void write(ByteBuf buffer) {
		if (ChunkCapabilities.hasBlockCount()) {
			buffer.writeShort(this.blockCount);
		}

		buffer.writeByte(this.bitsPerBlock);
		this.palette.write(buffer);

		long[] data = this.data.toArray();
		ByteBufUtil.writeVarInt(buffer, data.length);
		for (long entry : data) {
			buffer.writeLong(entry);
		}
	}

	public int[] read(ByteBuf buffer) {
		if (ChunkCapabilities.hasBlockCount()) {
			this.blockCount = buffer.readShort();
		}

		this.setBitsPerBlock(buffer.readUnsignedByte(), false);

		this.palette.read(buffer);

		long[] data = this.data.toArray();
		int length = ByteBufUtil.readVarInt(buffer);
		if (data.length != length) {
			throw new IndexOutOfBoundsException("data.length != VarBitBuffer::size " + length + " " + this.data);
		}

		for (int i = 0; i < data.length; i++) {
			data[i] = buffer.readLong();
		}

		int[] directData = new int[4096];
		for (int i = 0; i < directData.length; i++) {
			directData[i] = this.getBlock(i);
		}
		return directData;
	}
}
