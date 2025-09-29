package dev.imprex.orebfuscator.chunk;

public class ZeroVarBitBuffer implements VarBitBuffer {

  public static final long[] EMPTY = new long[0];

  private final int size;

  public ZeroVarBitBuffer(int size) {
    this.size = size;
  }

  @Override
  public int get(int index) {
    return 0;
  }

  @Override
  public void set(int index, int value) {
    if (value != 0) {
      throw new IllegalArgumentException("ZeroVarBitBuffer can't hold any value");
    }
  }

  @Override
  public long[] toArray() {
    return EMPTY;
  }

  @Override
  public int size() {
    return this.size;
  }
}
