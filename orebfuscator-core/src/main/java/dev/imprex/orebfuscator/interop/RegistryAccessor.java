package dev.imprex.orebfuscator.interop;

import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockTag;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface RegistryAccessor {

  int getUniqueBlockStateCount();

  int getMaxBitsPerBlockState();

  boolean isAir(int blockId);

  boolean isOccluding(int blockId);

  boolean isBlockEntity(int blockId);

  @Nullable BlockProperties getBlockByName(String name);

  @Nullable BlockTag getBlockTagByName(String name);

}
