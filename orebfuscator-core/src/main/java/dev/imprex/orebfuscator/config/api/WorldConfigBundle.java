package dev.imprex.orebfuscator.config.api;

import java.util.random.RandomGenerator;
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

  int nextRandomObfuscationBlock(RandomGenerator random, int y);

  int nextRandomProximityBlock(RandomGenerator random, int y);
}
