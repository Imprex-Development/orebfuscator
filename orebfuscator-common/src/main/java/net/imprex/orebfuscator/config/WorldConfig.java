package net.imprex.orebfuscator.config;

public interface WorldConfig {

	boolean isEnabled();

	int getMinY();

	int getMaxY();

	boolean matchesWorldName(String worldName);

	int nextRandomBlockState();

}
