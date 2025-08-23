package dev.imprex.orebfuscator.config.api;

import dev.imprex.orebfuscator.interop.WorldAccessor;

public interface Config {

  byte[] systemHash();

  String report();

  GeneralConfig general();

  AdvancedConfig advanced();

  CacheConfig cache();

  WorldConfigBundle world(WorldAccessor world);

  boolean proximityEnabled();
}
