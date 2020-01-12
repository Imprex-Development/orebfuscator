/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.chunkmap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Stack;

import net.imprex.orebfuscator.NmsInstance;

public class ChunkMapManager implements AutoCloseable {

	private static final Object _lock = new Object();
	private static final Stack<ChunkMapBuffer> _bufferStack = new Stack<>();

	private static ChunkMapBuffer popBuffer() {
		synchronized (_lock) {
			return _bufferStack.isEmpty() ? new ChunkMapBuffer() : _bufferStack.pop();
		}
	}

	private static void pushBuffer(ChunkMapBuffer buffer) {
		synchronized (_lock) {
			_bufferStack.push(buffer);
		}
	}

	private ChunkMapBuffer buffer;
	private ChunkData chunkData;
	private ChunkReader reader;
	private int sectionCount;
	private int sectionIndex;
	private int y;
	private int minX;
	private int maxX;
	private int minZ;
	private int maxZ;
	private int blockIndex;

	public int getSectionCount() {
		return this.sectionCount;
	}

	public int getY() {
		return this.y;
	}

	public ChunkData getChunkData() {
		return this.chunkData;
	}

	private ChunkMapManager() {

	}

	public static ChunkMapManager create(ChunkData chunkData) throws IOException {
		ChunkMapManager manager = new ChunkMapManager();
		manager.chunkData = chunkData;
		manager.buffer = popBuffer();
		manager.reader = new ChunkReader(chunkData.data);
		manager.sectionCount = 0;
		manager.sectionIndex = -1;
		manager.minX = chunkData.chunkX << 4;
		manager.maxX = manager.minX + 15;
		manager.minZ = chunkData.chunkZ << 4;
		manager.maxZ = manager.minZ + 15;

		manager.buffer.lightArrayLength = NmsInstance.get().hasLightArray() ? 2048 : 0;

		if (chunkData.isOverworld) {
			manager.buffer.lightArrayLength <<= 1;
		}

		manager.buffer.writer.init();

		int mask = chunkData.primaryBitMask;

		while (mask != 0) {
			if ((mask & 0x1) != 0) {
				manager.sectionCount++;
			}

			mask >>>= 1;
		}

		manager.buffer.clearLayers();

		manager.moveToNextLayer();

		return manager;
	}

	@Override
	public void close() throws IOException {
		pushBuffer(this.buffer);
	}

	public boolean inputHasNonAirBlock() {
		return this.buffer.paletteLength > 1 || NmsInstance.get().isAir(this.buffer.palette[0]);
	}

	public boolean initOutputPalette() {
		if (this.buffer.paletteLength == 0 || this.buffer.paletteLength == 255) {
			this.buffer.outputPaletteLength = 0;
			return false;
		}

		Arrays.fill(this.buffer.outputPaletteMap, (byte) -1);

		this.buffer.outputPaletteLength = this.buffer.paletteLength;

		for (int i = 0; i < this.buffer.paletteLength; i++) {
			int blockData = this.buffer.palette[i];

			this.buffer.outputPalette[i] = blockData;

			if (blockData >= 0) {
				this.buffer.outputPaletteMap[blockData] = (byte) i;
			}
		}

		return true;
	}

	public boolean addToOutputPalette(int blockData) {
		if (this.buffer.outputPaletteMap[blockData] >= 0) {
			return true;
		}

		// 255 (-1 for byte) is special code in my algorithm
		if (this.buffer.outputPaletteLength == 254) {
			this.buffer.outputPaletteLength = 0;
			return false;
		}

		this.buffer.outputPalette[this.buffer.outputPaletteLength] = blockData;
		this.buffer.outputPaletteMap[blockData] = (byte) this.buffer.outputPaletteLength;

		this.buffer.outputPaletteLength++;

		return true;
	}

	public void initOutputSection() throws IOException {
		this.calcOutputBitsPerBlock();

		this.buffer.writer.setBitsPerBlock(this.buffer.outputBitsPerBlock);

		// Block count
		if (NmsInstance.get().hasBlockCount()) {
			this.buffer.writer.writeShort((short) this.buffer.blockCount);
		}

		// Bits Per Block
		this.buffer.writer.writeByte((byte) this.buffer.outputBitsPerBlock);

		// Palette Length
		this.buffer.writer.writeVarInt(this.buffer.outputPaletteLength);

		// Palette
		for (int i = 0; i < this.buffer.outputPaletteLength; i++) {
			this.buffer.writer.writeVarInt(this.buffer.outputPalette[i]);
		}

		int dataArrayLengthInBits = this.buffer.outputBitsPerBlock << 12;// multiply by 4096
		int outputDataArrayLength = dataArrayLengthInBits >>> 6;// divide by 64

		if ((dataArrayLengthInBits & 0x3f) != 0) {
			outputDataArrayLength++;
		}

		// Data Array Length
		this.buffer.writer.writeVarInt(outputDataArrayLength);

		// Copy Block Light and Sky Light arrays
		int lightArrayStartIndex = this.buffer.dataArrayStartIndex + (this.buffer.dataArrayLength << 3);
		int outputLightArrayStartIndex = this.buffer.writer.getByteIndex() + (outputDataArrayLength << 3);

		System.arraycopy(this.chunkData.data, lightArrayStartIndex, this.buffer.output, outputLightArrayStartIndex,
				this.buffer.lightArrayLength);
	}

	public void writeOutputBlock(int blockData) throws IOException {
		if (this.buffer.outputPaletteLength > 0) {
			long paletteIndex = this.buffer.outputPaletteMap[blockData] & 0xffL;

			if (paletteIndex == 255) {
				throw new IllegalArgumentException("Block " + blockData + " is absent in output palette.");
			}

			this.buffer.writer.writeBlockBits(paletteIndex);
		} else {
			this.buffer.writer.writeBlockBits(blockData);
		}
	}

	public void finalizeOutput() throws IOException {
		if (this.buffer.writer.getByteIndex() == 0) {
			return;
		}

		this.buffer.writer.save();
		this.buffer.writer.skip(this.buffer.lightArrayLength);
	}

	public byte[] createOutput() {
		int readerByteIndex = this.reader.getByteIndex();
		int writerByteIndex = this.buffer.writer.getByteIndex();
		int biomesSize = this.chunkData.data.length - readerByteIndex;
		byte[] output = new byte[writerByteIndex + biomesSize];

		System.arraycopy(this.buffer.output, 0, output, 0, writerByteIndex);

		if (biomesSize > 0) {
			System.arraycopy(this.chunkData.data, readerByteIndex, output, writerByteIndex, biomesSize);
		}

		return output;
	}

	private void calcOutputBitsPerBlock() {
		if (this.buffer.outputPaletteLength == 0) {
			this.buffer.outputBitsPerBlock = ChunkMapBuffer.getBitsPerBlock();
		} else {
			byte mask = (byte) this.buffer.outputPaletteLength;
			int index = 0;

			while ((mask & 0x80) == 0) {
				index++;
				mask <<= 1;
			}

			this.buffer.outputBitsPerBlock = 8 - index;

			if (this.buffer.outputBitsPerBlock < 4) {
				this.buffer.outputBitsPerBlock = 4;
			}
		}
	}

	public int readNextBlock() throws IOException {
		if (this.blockIndex == 16 * 16) {
			if (!this.moveToNextLayer()) {
				return -1;
			}
		}

		return this.buffer.curLayer.map[this.blockIndex++];
	}

	public int get(int x, int y, int z) throws IOException {
		if (x < this.minX || x > this.maxX || z < this.minZ || z > this.maxZ || y > 255 || y < this.y - 1
				|| y > this.y + 1) {
			return -1;
		}

		ChunkLayer layer;

		if (y == this.y) {
			layer = this.buffer.curLayer;
		} else if (y == this.y - 1) {
			layer = this.buffer.prevLayer;
		} else {
			layer = this.buffer.nextLayer;
		}

		if (!layer.hasData) {
			return -1;
		}

		int blockIndex = z - this.minZ << 4 | x - this.minX;

		return layer.map[blockIndex];
	}

	private boolean moveToNextLayer() throws IOException {
		if (!this.increaseY()) {
			return false;
		}

		this.shiftLayersDown();

		if (!this.buffer.curLayer.hasData) {
			this.readLayer(this.buffer.curLayer);
		}

		if (this.y + 1 >>> 4 > this.sectionIndex) {
			int oldSectionIndex = this.sectionIndex;

			this.moveToNextSection();

			if (this.sectionIndex < 16 && oldSectionIndex + 1 == this.sectionIndex) {
				this.readLayer(this.buffer.nextLayer);
			}
		} else {
			this.readLayer(this.buffer.nextLayer);
		}

		this.blockIndex = 0;

		return true;
	}

	private boolean increaseY() throws IOException {
		if (this.sectionIndex < 0) {
			if (!this.moveToNextSection()) {
				return false;
			}

			this.y = this.sectionIndex << 4;
		} else {
			this.y++;

			if ((this.y & 0xf) == 0) {
				if (this.sectionIndex > 15) {
					return false;
				}

				if (this.y >>> 4 != this.sectionIndex) {
					this.buffer.clearLayers();
					this.y = this.sectionIndex << 4;
				}
			}
		}

		return true;
	}

	private void shiftLayersDown() {
		ChunkLayer temp = this.buffer.prevLayer;

		this.buffer.prevLayer = this.buffer.curLayer;
		this.buffer.curLayer = this.buffer.nextLayer;
		this.buffer.nextLayer = temp;
		this.buffer.nextLayer.hasData = false;
	}

	private boolean moveToNextSection() throws IOException {
		if (this.sectionIndex >= 0) {
			this.reader.skip(this.buffer.lightArrayLength);
		}

		do {
			this.sectionIndex++;
		} while (this.sectionIndex < 16 && (this.chunkData.primaryBitMask & 1 << this.sectionIndex) == 0);

		if (this.sectionIndex >= 16) {
			return false;
		}

		this.readSectionHeader();

		return true;
	}

	private void readLayer(ChunkLayer layer) throws IOException {
		for (int i = 0; i < 16 * 16; i++) {
			int blockData = this.reader.readBlockBits();

			if (this.buffer.paletteLength > 0) {
				blockData = blockData >= 0 && blockData < this.buffer.paletteLength ? this.buffer.palette[blockData]
						: NmsInstance.get().getCaveAirBlockId();
			}

			layer.map[i] = blockData;
		}

		layer.hasData = true;
	}

	private void readSectionHeader() throws IOException {
		if (NmsInstance.get().hasBlockCount()) {
			this.buffer.blockCount = this.reader.readShort();
		}

		this.buffer.bitsPerBlock = this.reader.readByte();
		this.buffer.paletteLength = this.reader.readVarInt();

		for (int i = 0; i < this.buffer.paletteLength; i++) {
			int paletteData = this.reader.readVarInt();

			this.buffer.palette[i] = paletteData;
		}

		this.buffer.dataArrayLength = this.reader.readVarInt();

		this.buffer.dataArrayStartIndex = this.reader.getByteIndex();

		this.reader.setBitsPerBlock(this.buffer.bitsPerBlock);
	}
}