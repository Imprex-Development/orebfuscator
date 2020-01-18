package net.imprex.orebfuscator.config;

import org.bukkit.World;

public interface Config {

	GeneralConfig general();

	CacheConfig cache();

	WorldConfig world(World world);
}
