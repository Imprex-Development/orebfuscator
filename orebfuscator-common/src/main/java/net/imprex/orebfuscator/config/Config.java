package net.imprex.orebfuscator.config;

import java.util.List;

import org.bukkit.World;

public interface Config {

	GeneralConfig general();

	CacheConfig cache();

	List<WorldConfig> worlds();

	WorldConfig world(World world);
}
