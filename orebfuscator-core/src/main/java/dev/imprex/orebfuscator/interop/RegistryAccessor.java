package dev.imprex.orebfuscator.interop;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockTag;

@NullMarked
public interface RegistryAccessor {

  int getUniqueBlockStateCount();

  int getMaxBitsPerBlockState();

  boolean isAir(int blockId);

  boolean isLava(int blockId);

  boolean isOccluding(int blockId);

  boolean isBlockEntity(int blockId);

  @Nullable BlockProperties getBlockByName(String name);

  @Nullable BlockTag getBlockTagByName(String name);

}
