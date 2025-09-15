package dev.imprex.orebfuscator.chunk;

public record ZeroVarBitBuffer(int size) implements VarBitBuffer {

  public static final long[] EMPTY = new long[0];

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
}
