package net.imprex.orebfuscator.config;

import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;

public interface WorldConfig {

	boolean enabled();

	List<World> worlds();

	Set<Material> darknessBlocks(); // if disabled return emptySet

	Set<Integer> randomBlockId();

	Set<Material> hiddenBlocks();
}
