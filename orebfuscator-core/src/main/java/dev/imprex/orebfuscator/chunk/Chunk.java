package dev.imprex.orebfuscator.chunk;

import java.util.Arrays;
import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

public class Chunk implements AutoCloseable {

  private final ChunkFactory factory;

  private final int chunkX;
  private final int chunkZ;

  private final WorldAccessor worldAccessor;
  private final ChunkSectionHolder[] sections;

  private final ByteBuf inputBuffer;
  private final ByteBuf outputBuffer;

  Chunk(ChunkFactory factory, ChunkPacketAccessor packet) {
    this.factory = factory;

    this.chunkX = packet.chunkX();
    this.chunkZ = packet.chunkZ();

    this.worldAccessor = packet.world();
    this.sections = new ChunkSectionHolder[this.worldAccessor.getSectionCount()];

    byte[] data = packet.data();
    this.inputBuffer = Unpooled.wrappedBuffer(data);
    this.outputBuffer = PooledByteBufAllocator.DEFAULT.heapBuffer(data.length);

    for (int sectionIndex = 0; sectionIndex < this.sections.length; sectionIndex++) {
      if (packet.isSectionPresent(sectionIndex)) {
        this.sections[sectionIndex] = new ChunkSectionHolder();
      }
    }
  }

  public WorldAccessor world() {
    return worldAccessor;
  }

  public int getSectionCount() {
    return this.sections.length;
  }

  public ChunkSection getSection(int index) {
    ChunkSectionHolder chunkSection = this.sections[index];
    if (chunkSection != null) {
      return chunkSection.chunkSection;
    }
    return null;
  }

  public int getBlockState(int x, int y, int z) {
    if (x >> 4 == this.chunkX && z >> 4 == this.chunkZ) {
      ChunkSectionHolder chunkSection = this.sections[this.worldAccessor.getSectionIndex(y)];
      if (chunkSection != null) {
        return chunkSection.data[ChunkSection.positionToIndex(x & 0xF, y & 0xF, z & 0xF)];
      }
      return 0;
    }

    return -1;
  }

  public byte[] finalizeOutput() {
    for (ChunkSectionHolder chunkSection : this.sections) {
      if (chunkSection != null) {
        chunkSection.write();
      }
    }

    this.outputBuffer.writeBytes(this.inputBuffer);

    return Arrays.copyOfRange(
        this.outputBuffer.array(), this.outputBuffer.arrayOffset(),
        this.outputBuffer.arrayOffset() + this.outputBuffer.readableBytes());
  }

  @Override
  public void close() throws Exception {
    this.inputBuffer.release();
    this.outputBuffer.release();
  }

  private void skipBiomePalettedContainer() {
    int bitsPerValue = this.inputBuffer.readUnsignedByte();

    if (bitsPerValue == 0) {
      ByteBufUtil.readVarInt(this.inputBuffer);
    } else if (bitsPerValue <= 3) {
      for (int i = ByteBufUtil.readVarInt(this.inputBuffer); i > 0; i--) {
        ByteBufUtil.readVarInt(this.inputBuffer);
      }
    }

    int expectedDataLength = SimpleVarBitBuffer.calculateArraySize(bitsPerValue, 64);

    if (factory.versionFlags().hasLongArrayLengthField()) {
      int dataLength = ByteBufUtil.readVarInt(this.inputBuffer);
      if (expectedDataLength != dataLength) {
        throw new IndexOutOfBoundsException(
            "data.length != VarBitBuffer::size " + dataLength + " " + expectedDataLength);
      }
    }

    this.inputBuffer.skipBytes(Long.BYTES * expectedDataLength);
  }

  private class ChunkSectionHolder {

    public final ChunkSection chunkSection;

    public final int[] data;
    public final int extraOffset;

    private int extraBytes;

    public ChunkSectionHolder() {
      this.chunkSection = new ChunkSection(factory);

      this.data = this.chunkSection.read(inputBuffer);
      this.extraOffset = inputBuffer.readerIndex();

      if (factory.versionFlags().hasBiomePalettedContainer()) {
        skipBiomePalettedContainer();
        this.extraBytes = inputBuffer.readerIndex() - this.extraOffset;
      }
    }

    public void write() {
      this.chunkSection.write(outputBuffer);
      if (this.extraBytes > 0) {
        outputBuffer.writeBytes(inputBuffer, this.extraOffset, extraBytes);
      }
    }
  }
}
