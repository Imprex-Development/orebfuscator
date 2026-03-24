package dev.imprex.orebfuscator.chunk;

import dev.imprex.orebfuscator.interop.ServerAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;

public final class ChunkVersionFlags {

  // hasFluidCount >= 26.1.0
  // hasLongArrayLengthField < 1.21.5
  // hasBiomePalettedContainer >= 1.18
  // hasSingleValuePalette >= 1.18

  private final boolean hasFluidCount;
  private final boolean hasLongArrayLengthField;
  private final boolean hasBiomePalettedContainer;
  private final boolean hasSingleValuePalette;

  public ChunkVersionFlags(ServerAccessor serverAccessor) {
    var version = serverAccessor.minecraftVersion();
    hasFluidCount = version.isAtOrAbove("26.1.0");
    hasLongArrayLengthField = version.isBelow("1.21.5");
    hasBiomePalettedContainer = version.isAtOrAbove("1.18");
    hasSingleValuePalette = version.isAtOrAbove("1.18");

    OfcLogger.debug("MinecraftVersion: " + version);
    OfcLogger.debug("ChunkVersionFlags - hasFluidCount: " + hasFluidCount);
    OfcLogger.debug("ChunkVersionFlags - hasLongArrayLengthField: " + hasLongArrayLengthField);
    OfcLogger.debug("ChunkVersionFlags - hasBiomePalettedContainer: " + hasBiomePalettedContainer);
    OfcLogger.debug("ChunkVersionFlags - hasSingleValuePalette: " + hasSingleValuePalette);
  }

  public boolean hasFluidCount() {
    return hasFluidCount;
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
