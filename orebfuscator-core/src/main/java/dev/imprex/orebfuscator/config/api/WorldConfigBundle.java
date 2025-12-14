package dev.imprex.orebfuscator.config.api;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface WorldConfigBundle {

  BlockFlags blockFlags();

  @Nullable ObfuscationConfig obfuscation();

  @Nullable ProximityConfig proximity();

  boolean needsObfuscation();

  int minSectionIndex();

  int maxSectionIndex();

  boolean shouldObfuscate(int y);

  int nextRandomObfuscationBlock(int y);

  int nextRandomProximityBlock(int y);
}
