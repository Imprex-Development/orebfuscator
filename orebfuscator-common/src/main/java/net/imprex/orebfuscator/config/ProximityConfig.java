package net.imprex.orebfuscator.config;

import java.util.Map;

import net.imprex.orebfuscator.util.BlockProperties;

public interface ProximityConfig extends WorldConfig {

	int distance();

	boolean useFastGazeCheck();

	Iterable<Map.Entry<BlockProperties, Integer>> hiddenBlocks();
}
