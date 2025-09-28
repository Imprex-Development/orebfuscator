package net.imprex.orebfuscator.chunk;

public interface VarBitBuffer {

  int get(int index);

  void set(int index, int value);

  long[] toArray();

  int size();
}
