package dev.imprex.orebfuscator.config.api;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface WorldConfig {

  boolean isEnabled();

  int getMinY();

  int getMaxY();

  boolean matchesWorldName(String worldName);

  boolean shouldObfuscate(int y);

}
