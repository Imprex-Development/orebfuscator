package dev.imprex.orebfuscator.config.api;

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
