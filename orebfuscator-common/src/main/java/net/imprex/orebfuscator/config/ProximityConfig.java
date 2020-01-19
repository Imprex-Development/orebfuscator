package net.imprex.orebfuscator.config;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.bukkit.World;

public interface ProximityConfig {

	boolean enabled();

	List<World> worlds();

	int distance();

	int distanceSquared();

	boolean useFastGazeCheck();

	Collection<Integer> randomBlocks();

	int randomBlockId();

	Set<Integer> hiddenBlocks();

	boolean shouldHide(int y, int id);
}
