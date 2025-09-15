package dev.imprex.orebfuscator.chunk;

import java.util.Arrays;
import io.netty.buffer.ByteBuf;

public class IndirectPalette implements Palette {

  private final int bitsPerValue;
  private final ChunkSection chunkSection;

  private final byte[] byValue;
  private final int[] byId;

  private int size = 0;

  public IndirectPalette(int bitsPerValue, ChunkSection chunkSection) {
    this.bitsPerValue = bitsPerValue;
    this.chunkSection = chunkSection;

    // TODO improve block to index
    this.byValue = new byte[chunkSection.registryAccessor().getUniqueBlockStateCount()];
    Arrays.fill(this.byValue, (byte) 0xFF);
    this.byId = new int[1 << bitsPerValue];
  }

  @Override
  public int idFor(int value) {
    int id = this.byValue[value] & 0xFF;
    if (id == 0xFF) {
      id = this.size++;

      if (id != 0xFF && id < this.byId.length) {
        this.byValue[value] = (byte) id;
        this.byId[id] = value;
      } else {
        id = this.chunkSection.grow(this.bitsPerValue + 1, value);
      }
    }
    return id;
  }

  @Override
  public int valueFor(int id) {
    if (id < 0 || id >= this.size) {
      throw new IndexOutOfBoundsException();
    } else {
      return this.byId[id];
    }
  }

  @Override
  public void read(ByteBuf buffer) {
    this.size = ByteBufUtil.readVarInt(buffer);
    for (int id = 0; id < size; id++) {
      int value = ByteBufUtil.readVarInt(buffer);
      this.byId[id] = value;
      this.byValue[value] = (byte) id;
    }
  }

  @Override
  public void write(ByteBuf buffer) {
    ByteBufUtil.writeVarInt(buffer, this.size);

    for (int id = 0; id < this.size; id++) {
      ByteBufUtil.writeVarInt(buffer, this.valueFor(id));
    }
  }
}
