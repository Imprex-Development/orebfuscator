package dev.imprex.orebfuscator.config.api;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface AdvancedConfig {

  int threads();

  boolean hasObfuscationTimeout();

  long obfuscationTimeout();

  int proximityDefaultBucketSize();

  int proximityThreadCheckInterval();

  boolean hasProximityPlayerCheckInterval();

  int proximityPlayerCheckInterval();
}
