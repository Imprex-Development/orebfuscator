package net.imprex.orebfuscator.config;

import java.util.Set;

public interface ProximityHiderConfig {

	boolean enabled();

	int distance();

	int distanceSquared();

	Set<Integer> randomBlockId();

	Set<Integer> hiddenBlocks();

	boolean shouldHide(int y, int id);
}
