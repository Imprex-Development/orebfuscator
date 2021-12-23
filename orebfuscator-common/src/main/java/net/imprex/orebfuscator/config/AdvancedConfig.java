package net.imprex.orebfuscator.config;

public interface AdvancedConfig {

	int maxMillisecondsPerTick();

	int protocolLibThreads();

	int obfuscationWorkerThreads();

	int proximityHiderThreads();
}
