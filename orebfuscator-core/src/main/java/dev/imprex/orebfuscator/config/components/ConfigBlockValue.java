package dev.imprex.orebfuscator.config.components;

import java.util.Collections;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockTag;

public record ConfigBlockValue(@NotNull String value, @NotNull Set<BlockProperties> blocks) {

  @NotNull
  public static ConfigBlockValue invalid(@NotNull String value) {
    return new ConfigBlockValue(value, Collections.emptySet());
  }

  @NotNull
  public static ConfigBlockValue block(@NotNull BlockProperties block) {
    return new ConfigBlockValue(block.getKey().toString(), Set.of(block));
  }

  @NotNull
  public static ConfigBlockValue invalidTag(@NotNull String value) {
    return new ConfigBlockValue(String.format("tag(%s)", value), Collections.emptySet());
  }

  @NotNull
  public static ConfigBlockValue tag(@NotNull BlockTag tag, @NotNull Set<BlockProperties> blocks) {
    return new ConfigBlockValue(String.format("tag(%s)", tag.key().toString()), Collections.unmodifiableSet(blocks));
  }
}
