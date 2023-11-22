package net.imprex.orebfuscator.config;

import net.imprex.orebfuscator.util.BlockProperties;

public interface ObfuscationConfig extends WorldConfig {

	boolean layerObfuscation();

	Iterable<BlockProperties> hiddenBlocks();
}
