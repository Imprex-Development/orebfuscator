package net.imprex.orebfuscator.config;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;

public interface ProximityConfig {

	void initialize();

	boolean enabled();
	void enabled(boolean enabled);

	List<String> worlds();

	int distance();
	/**
	 * @param distance
	 * @throws IllegalArgumentException When the distance value is lower than one
	 */
	void distance(int distance);

	int distanceSquared();

	boolean useFastGazeCheck();
	void useFastGazeCheck(boolean fastGaze);

	Map<Material, Short> hiddenBlocks();

	Map<Material, Integer> randomBlocks();

	int randomBlockId();
}
