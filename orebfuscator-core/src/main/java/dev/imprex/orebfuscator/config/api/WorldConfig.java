package dev.imprex.orebfuscator.config.api;

public interface WorldConfig {

	boolean isEnabled();

	int getMinY();

	int getMaxY();

	boolean matchesWorldName(String worldName);

	boolean shouldObfuscate(int y);

}
