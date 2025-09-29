package dev.imprex.orebfuscator.chunk;

import io.netty.buffer.ByteBuf;

public interface Palette {

  int idFor(int value);

  int valueFor(int id);

  void read(ByteBuf buffer);

  void write(ByteBuf buffer);

}
