package dev.imprex.orebfuscator.config.api;

import dev.imprex.orebfuscator.util.BlockProperties;

public interface ObfuscationConfig extends WorldConfig {

	boolean layerObfuscation();

	Iterable<BlockProperties> hiddenBlocks();
}
