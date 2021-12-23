package net.imprex.orebfuscator.config;

import org.bukkit.World;

public interface Config {

	GeneralConfig general();

	AdvancedConfig advanced();

	CacheConfig cache();

	WorldConfigBundle bundle(World world);

	BlockFlags blockFlags(World world);

	boolean needsObfuscation(World world);

	ObfuscationConfig obfuscation(World world);
	
	boolean proximityEnabled();

	ProximityConfig proximity(World world);

	byte[] systemHash();
}
