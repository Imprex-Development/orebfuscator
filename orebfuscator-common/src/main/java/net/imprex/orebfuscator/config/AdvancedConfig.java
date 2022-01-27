package net.imprex.orebfuscator.config;

public interface AdvancedConfig {

	boolean useAsyncPacketListener();

	int maxMillisecondsPerTick();

	int protocolLibThreads();

	int obfuscationWorkerThreads();

	int proximityHiderThreads();
}
