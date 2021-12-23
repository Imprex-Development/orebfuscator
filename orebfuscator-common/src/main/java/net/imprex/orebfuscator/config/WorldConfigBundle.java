package net.imprex.orebfuscator.config;

public interface WorldConfigBundle {

	BlockFlags blockFlags();

	ObfuscationConfig obfuscation();

	ProximityConfig proximity();

	int minSectionIndex();

	int maxSectionIndex();

	boolean shouldObfuscate(int y);
}
