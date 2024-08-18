package net.imprex.orebfuscator.config;

public interface AdvancedConfig {

	int obfuscationThreads();

	boolean hasObfuscationTimeout();

	long obfuscationTimeout();

	int maxMillisecondsPerTick();

	int proximityThreads();

	int proximityDefaultBucketSize();

	int proximityThreadCheckInterval();

	boolean hasProximityPlayerCheckInterval();

	int proximityPlayerCheckInterval();
}
