package net.imprex.orebfuscator.util;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;

public class BlockProperties {

  public static Builder builder(NamespacedKey key) {
    return new Builder(key);
  }

  private final NamespacedKey key;
  private final BlockStateProperties defaultBlockState;
  private final ImmutableList<BlockStateProperties> blockStates;

  private BlockProperties(Builder builder) {
    this.key = builder.key;
    this.defaultBlockState = builder.defaultBlockState;
    this.blockStates = ImmutableList.copyOf(builder.blockStates);
  }

  public NamespacedKey getKey() {
    return key;
  }

  public BlockStateProperties getDefaultBlockState() {
    return defaultBlockState;
  }

  public ImmutableList<BlockStateProperties> getBlockStates() {
    return blockStates;
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
    if (!(obj instanceof BlockProperties)) {
      return false;
    }
    BlockProperties other = (BlockProperties) obj;
    return Objects.equals(key, other.key);
  }

  @Override
  public String toString() {
    return "BlockProperties [key=" + key + ", defaultBlockState=" + defaultBlockState + ", blockStates="
        + blockStates + "]";
  }

  public static class Builder {

    private final NamespacedKey key;

    private BlockStateProperties defaultBlockState;
    private final Set<BlockStateProperties> blockStates = new HashSet<>();

    private Builder(NamespacedKey key) {
      this.key = key;
    }

    public Builder withBlockState(BlockStateProperties blockState) {
      if (!blockStates.add(blockState)) {
        throw new IllegalStateException(
            String.format("duplicate block state id (%s) for block: %s", blockState.getId(), key));
      }

      if (blockState.isDefaultState()) {
        // check for multiple default blocks
        if (this.defaultBlockState != null) {
          throw new IllegalStateException(
              String.format("multiple default block states for block: %s", blockState.getId(), key));
        }

        this.defaultBlockState = blockState;
      }

      return this;
    }

    public BlockProperties build() {
      Objects.requireNonNull(this.defaultBlockState, "missing default block state for block: " + this.key);

      if (this.blockStates.size() == 0) {
        throw new IllegalStateException("missing block states for block: " + this.key);
      }

      return new BlockProperties(this);
    }
  }
}
