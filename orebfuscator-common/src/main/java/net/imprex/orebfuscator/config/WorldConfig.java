package net.imprex.orebfuscator.config;

import java.util.List;
import java.util.Set;

import org.bukkit.Material;

public interface WorldConfig {

	boolean enabled();
	void enabled(boolean enabled);
	
	List<String> worlds();

	Set<Material> hiddenBlocks();

	Set<Material> randomBlocks();

	List<Integer> randomBlockIds();

	int randomBlockId();
}
