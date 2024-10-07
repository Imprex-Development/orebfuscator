package net.imprex.orebfuscator.config;

import org.bukkit.World;

public interface Config {

	byte[] systemHash();

	String report();

	GeneralConfig general();

	AdvancedConfig advanced();

	CacheConfig cache();

	WorldConfigBundle world(World world);

	boolean proximityEnabled();
}
