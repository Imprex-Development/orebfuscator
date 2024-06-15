package net.imprex.orebfuscator.config;

public interface AdvancedConfig {

	int maxMillisecondsPerTick();

	int protocolLibThreads();

	int obfuscationWorkerThreads();

	boolean hasObfuscationTimeout();

	int obfuscationTimeout();

	int proximityHiderThreads();

	int proximityDefaultBucketSize();

	int proximityThreadCheckInterval();

	boolean hasProximityPlayerCheckInterval();

	int proximityPlayerCheckInterval();
}
