package net.imprex.orebfuscator.config;

import org.bukkit.World;

public interface Config {

	GeneralConfig general();

	CacheConfig cache();

	BlockMask blockMask(World world);

	boolean needsObfuscation(World world);

	WorldConfig world(World world);
	
	boolean proximityEnabled();

	ProximityConfig proximity(World world);

	byte[] hash();
}
