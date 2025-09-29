package dev.imprex.orebfuscator.util;

public class BlockStateProperties {

  public static Builder builder(int id) {
    return new Builder(id);
  }

  private final int id;

  private final boolean isAir;
  private final boolean isOccluding;
  private final boolean isBlockEntity;
  private final boolean isDefaultState;

  private BlockStateProperties(Builder builder) {
    this.id = builder.id;
    this.isAir = builder.isAir;
    this.isOccluding = builder.isOccluding;
    this.isBlockEntity = builder.isBlockEntity;
    this.isDefaultState = builder.isDefaultState;
  }

  public int getId() {
    return id;
  }

  public boolean isAir() {
    return isAir;
  }

  public boolean isOccluding() {
    return isOccluding;
  }

  public boolean isBlockEntity() {
    return isBlockEntity;
  }

  public boolean isDefaultState() {
    return isDefaultState;
  }

  @Override
  public int hashCode() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof BlockStateProperties)) {
      return false;
    }
    BlockStateProperties other = (BlockStateProperties) obj;
    return id == other.id;
  }

  @Override
  public String toString() {
    return "BlockStateProperties [id=" + id + ", isDefaultState=" + isDefaultState + ", isAir=" + isAir
        + ", isOccluding=" + isOccluding + ", isBlockEntity=" + isBlockEntity + "]";
  }

  public static class Builder {

    private final int id;

    private boolean isAir;
    private boolean isOccluding;
    private boolean isBlockEntity;
    private boolean isDefaultState;

    private Builder(int id) {
      this.id = id;
    }

    public Builder withIsAir(boolean isAir) {
      this.isAir = isAir;
      return this;
    }

    public Builder withIsOccluding(boolean isOccluding) {
      this.isOccluding = isOccluding;
      return this;
    }

    public Builder withIsBlockEntity(boolean isBlockEntity) {
      this.isBlockEntity = isBlockEntity;
      return this;
    }

    public Builder withIsDefaultState(boolean isDefaultState) {
      this.isDefaultState = isDefaultState;
      return this;
    }

    public BlockStateProperties build() {
      return new BlockStateProperties(this);
    }
  }
}
