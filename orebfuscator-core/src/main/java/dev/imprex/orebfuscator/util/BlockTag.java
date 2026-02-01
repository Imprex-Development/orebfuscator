package dev.imprex.orebfuscator.util;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record BlockTag(NamespacedKey key, Set<BlockProperties> blocks) {

  public BlockTag(NamespacedKey key, Set<BlockProperties> blocks) {
    this.key = Objects.requireNonNull(key);
    this.blocks = Collections.unmodifiableSet(Objects.requireNonNull(blocks));
  }

  @Override
  public int hashCode() {
    return this.key.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof BlockTag other)) {
      return false;
    }
    return Objects.equals(key, other.key);
  }
}
