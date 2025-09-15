package dev.imprex.orebfuscator.chunk;

import dev.imprex.orebfuscator.interop.ServerAccessor;

public final class ChunkVersionFlags {

  // hasLongArrayLengthField < 1.21.5
  // hasBiomePalettedContainer >= 1.18
  // hasSingleValuePalette >= 1.18

  private final boolean hasLongArrayLengthField;
  private final boolean hasBiomePalettedContainer;
  private final boolean hasSingleValuePalette;

  public ChunkVersionFlags(ServerAccessor serverAccessor) {
    var version = serverAccessor.getMinecraftVersion();
    hasLongArrayLengthField = version.isBelow("1.21.5");
    hasBiomePalettedContainer = version.isAtOrAbove("1.18");
    hasSingleValuePalette = version.isAtOrAbove("1.18");
  }

  public boolean hasLongArrayLengthField() {
    return hasLongArrayLengthField;
  }

  public boolean hasBiomePalettedContainer() {
    return hasBiomePalettedContainer;
  }

  public boolean hasSingleValuePalette() {
    return hasSingleValuePalette;
  }
}
