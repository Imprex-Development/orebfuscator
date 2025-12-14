package dev.imprex.orebfuscator.config.api;

import dev.imprex.orebfuscator.interop.WorldAccessor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface Config {

  byte[] systemHash();

  @Nullable String report();

  GeneralConfig general();

  AdvancedConfig advanced();

  CacheConfig cache();

  WorldConfigBundle world(WorldAccessor world);

  boolean proximityEnabled();
}
