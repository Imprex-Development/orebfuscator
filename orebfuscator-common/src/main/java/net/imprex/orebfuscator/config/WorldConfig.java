package net.imprex.orebfuscator.config;

import java.util.Collection;
import java.util.List;

import org.bukkit.World;

public interface WorldConfig {

	public static final int BLOCK_MASK_OBFUSCATE = 1;
	public static final int BLOCK_MASK_DARKNESS = 2;
	public static final int BLOCK_MASK_TILEENTITY = 4;

	boolean enabled();
	
	List<World> worlds();

	int blockmask(int id);

	boolean darknessBlocksEnabled();

	Collection<Integer> randomBlocks();

	int randomBlockId();
}
