package dev.imprex.orebfuscator.config.api;

import dev.imprex.orebfuscator.config.components.ConfigBlockValue;

public interface ObfuscationConfig extends WorldConfig {

  boolean layerObfuscation();

  Iterable<ConfigBlockValue> hiddenBlocks();
}
