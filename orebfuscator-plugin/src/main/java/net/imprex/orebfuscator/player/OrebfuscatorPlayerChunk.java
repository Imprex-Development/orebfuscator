package net.imprex.orebfuscator.player;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.imprex.orebfuscator.util.BlockPos;

public class OrebfuscatorPlayerChunk {

  private final int chunkX;
  private final int chunkZ;

  private int proximitySize;
  private final int[] proximityBlocks;

  public OrebfuscatorPlayerChunk(int chunkX, int chunkZ, List<BlockPos> proximityBlocks) {
    this.chunkX = chunkX;
    this.chunkZ = chunkZ;

    this.proximitySize = proximityBlocks.size();
    this.proximityBlocks = new int[proximityBlocks.size()];

    for (int i = 0; i < proximityBlocks.size(); i++) {
      this.proximityBlocks[i] = proximityBlocks.get(i).toSectionPos();
    }
  }

  public boolean isEmpty() {
    return proximitySize <= 0;
  }

  public Iterator<BlockPos> proximityIterator() {
    return new ProximityItr();
  }

  private class ProximityItr implements Iterator<BlockPos> {

    private final int x = chunkX << 4;
    private final int z = chunkZ << 4;

    private int cursor;
    private int returnCursor = -1;

    @Override
    public boolean hasNext() {
      return cursor < proximitySize;
    }

    @Override
    public BlockPos next() {
      if (cursor >= proximitySize) {
        throw new NoSuchElementException();
      }

      int sectionPos = proximityBlocks[returnCursor = cursor];
      cursor++;

      return BlockPos.fromSectionPos(x, z, sectionPos);
    }

    @Override
    public void remove() {
      if (returnCursor < 0) {
        throw new IllegalStateException();
      }

      // remove entry
      final int index = returnCursor;
      final int newSize;
      if ((newSize = proximitySize - 1) > index) {
        System.arraycopy(proximityBlocks, index + 1, proximityBlocks, index, newSize - index);
      }
      proximityBlocks[proximitySize = newSize] = 0xFFFFFFFF;

      // update cursor positions
      cursor = returnCursor;
      returnCursor = -1;
    }
  }
}
