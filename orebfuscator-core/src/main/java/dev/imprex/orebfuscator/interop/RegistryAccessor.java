package dev.imprex.orebfuscator.interop;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockTag;

public interface RegistryAccessor {

  int getUniqueBlockStateCount();

  int getMaxBitsPerBlockState();

  boolean isAir(int blockId);

  boolean isOccluding(int blockId);

  boolean isBlockEntity(int blockId);

  @Nullable BlockProperties getBlockByName(@NotNull String name);

  @Nullable BlockTag getBlockTagByName(@NotNull String name);

}
