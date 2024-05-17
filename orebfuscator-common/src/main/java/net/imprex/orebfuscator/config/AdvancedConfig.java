package net.imprex.orebfuscator.config;

public interface AdvancedConfig {

	boolean useAsyncPacketListener();

	int maxMillisecondsPerTick();

	int protocolLibThreads();

	int obfuscationWorkerThreads();

	boolean hasObfuscationTimeout();

	long obfuscationTimeout();

	int proximityHiderThreads();

	int proximityDefaultBucketSize();

	int proximityThreadCheckInterval();

	boolean hasProximityPlayerCheckInterval();

	int proximityPlayerCheckInterval();
}
