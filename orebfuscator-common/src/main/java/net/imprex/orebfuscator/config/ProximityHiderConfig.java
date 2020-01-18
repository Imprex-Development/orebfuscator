package net.imprex.orebfuscator.config;

import java.util.Set;

import org.bukkit.Material;

public interface ProximityHiderConfig {

	boolean enabled();

	int distance();

	int distanceSquared();

	Material randomMaterial();

	Set<Integer> hiddenBlocks();

	boolean shouldHide(int y, int id);
}
