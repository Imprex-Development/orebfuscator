package dev.imprex.orebfuscator.config.api;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface AdvancedConfig {

  int obfuscationThreads();

  boolean hasObfuscationTimeout();

  long obfuscationTimeout();

  int maxMillisecondsPerTick();

  int proximityThreads();

  int proximityDefaultBucketSize();

  int proximityThreadCheckInterval();

  boolean hasProximityPlayerCheckInterval();

  int proximityPlayerCheckInterval();
}
