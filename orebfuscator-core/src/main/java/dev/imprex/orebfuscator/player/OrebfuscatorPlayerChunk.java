package dev.imprex.orebfuscator.player;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.util.BlockPos;

@NullMarked
public class OrebfuscatorPlayerChunk {

  private static final int FLAG_DELETED = 0x80;
  private static final int FLAG_LAVA_OBFUSCATED = 0x01;
  
  private final int chunkX;
  private final int chunkZ;

  private int proximitySize;
  private int[] proximityBlocks;
  private byte[] proximityFlags;

  public OrebfuscatorPlayerChunk(int chunkX, int chunkZ, List<ProximityBlock> proximityBlocks) {
    this.chunkX = chunkX;
    this.chunkZ = chunkZ;

    this.proximitySize = proximityBlocks.size();
    this.proximityBlocks = new int[proximityBlocks.size()];
    this.proximityFlags = new byte[proximityBlocks.size()];

    for (int i = 0; i < proximityBlocks.size(); i++) {
      var block = proximityBlocks.get(i);
      this.proximityBlocks[i] = block.blockPos().toSectionPos();
      this.proximityFlags[i] = (byte) (block.lavaObfuscated() ? FLAG_LAVA_OBFUSCATED : 0x00);
    }
  }

  public boolean isEmpty() {
    return this.proximitySize <= 0;
  }

  public ProximityIterator proximityIterator() {
    return new ProximityItr();
  }
  
  public interface ProximityIterator extends Iterator<ProximityBlock>, AutoCloseable {
    void close();
  }

  private class ProximityItr implements ProximityIterator {

    private final int x = chunkX << 4;
    private final int z = chunkZ << 4;

    private int cursor;
    private int removeCursor = -1;
    private int deleteCount;
    
    @Override
    public boolean hasNext() {
      return cursor < proximitySize;
    }

    @Override
    public ProximityBlock next() {
      if (cursor >= proximitySize) {
        throw new NoSuchElementException();
      }

      int sectionPos = proximityBlocks[removeCursor = cursor];
      int flags = proximityFlags[cursor++];
      
      var blockPos = BlockPos.fromSectionPos(x, z, sectionPos);
      return new ProximityBlock(blockPos, (flags & FLAG_LAVA_OBFUSCATED) != 0);
    }

    @Override
    public void remove() {
      if (removeCursor < 0) {
        throw new IllegalStateException();
      }

      // remove entry
      final int index = removeCursor;
      int flags = proximityFlags[index];
      if ((flags & FLAG_DELETED) != 0)
        throw new IllegalStateException("Already deleted!");

      // update cursor positions
      removeCursor = -1;
      
      proximityFlags[index] |= FLAG_DELETED;
      deleteCount++;
    }
    
    @Override
    public void close() {
      if (deleteCount > 0) {
        int newSize = Math.max(0, proximitySize - deleteCount);
        int[] newProximityBlocks = new int[newSize];
        byte[] newProximityFlags = new byte[newSize];
        
        int newIndex = 0;
        for (int oldIndex = 0; newIndex < newSize && oldIndex < proximitySize; oldIndex++) {
          if ((proximityFlags[oldIndex] & FLAG_DELETED) == 0) {
            newProximityBlocks[newIndex] = proximityBlocks[oldIndex];
            newProximityFlags[newIndex++] = proximityFlags[oldIndex];
          }
        }
        
        proximitySize = newSize;
        proximityBlocks = newProximityBlocks;
        proximityFlags = newProximityFlags;
      }
    }
  }
}
