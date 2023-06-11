package net.imprex.orebfuscator.config;

import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorAdvancedConfig implements AdvancedConfig {

	private boolean verbose = false;
	private boolean useAsyncPacketListener = true;
	private int maxMillisecondsPerTick = 10;
	private int protocolLibThreads = -1;

	private int obfuscationWorkerThreads = -1;
	private int obfuscationTimeout = 10_000;

	private int proximityHiderThreads = -1;
	private int proximityDefaultBucketSize = 50;
	private int proximityThreadCheckInterval = 50;
	private int proximityPlayerCheckInterval = 5000;

	private boolean protocolLibThreadsSet = false;
	private boolean obfuscationWorkerThreadsSet = false;
	private boolean hasObfuscationTimeout = false;
	private boolean proximityHiderThreadsSet = false;
	private boolean hasProximityPlayerCheckInterval = true;

	public void deserialize(ConfigurationSection section) {
		this.verbose = section.getBoolean("verbose", false);
		this.useAsyncPacketListener = section.getBoolean("useAsyncPacketListener", true);
		this.maxMillisecondsPerTick = section.getInt("maxMillisecondsPerTick", 10);

		if (this.maxMillisecondsPerTick <= 0 || this.maxMillisecondsPerTick >= 50) {
			throw new RuntimeException(
					"maxMillisecondsPerTick has to be between 0 and 50, value: " + this.maxMillisecondsPerTick);
		}

		this.protocolLibThreads = section.getInt("protocolLibThreads", -1);
		this.protocolLibThreadsSet = (this.protocolLibThreads > 0);

		this.obfuscationWorkerThreads = section.getInt("obfuscationWorkerThreads", -1);
		this.obfuscationWorkerThreadsSet = (this.obfuscationWorkerThreads > 0);

		this.obfuscationTimeout = section.getInt("obfuscationTimeout", -1);
		this.hasObfuscationTimeout = (this.obfuscationTimeout > 0);

		this.proximityHiderThreads = section.getInt("proximityHiderThreads", -1);
		this.proximityHiderThreadsSet = (this.proximityHiderThreads > 0);

		this.proximityDefaultBucketSize = section.getInt("proximityDefaultBucketSize", 50);
		if (proximityDefaultBucketSize <= 0) {
			throw new RuntimeException(
					"proximityDefaultBucketSize has to be bigger then 0, value: " + this.proximityDefaultBucketSize);
		}

		this.proximityThreadCheckInterval = section.getInt("proximityThreadCheckInterval", 50);
		if (this.proximityThreadCheckInterval <= 0) {
			throw new RuntimeException(
					"proximityThreadCheckInterval has to be bigger then 0, value: " + this.proximityThreadCheckInterval);
		}

		this.proximityPlayerCheckInterval = section.getInt("proximityPlayerCheckInterval", 5000);
		this.hasProximityPlayerCheckInterval = (this.proximityPlayerCheckInterval > 0);
	}

	public void initialize() {
		int availableThreads = Runtime.getRuntime().availableProcessors();
		this.protocolLibThreads = (int) (protocolLibThreadsSet ? protocolLibThreads : Math.ceil(availableThreads / 2f));
		this.obfuscationWorkerThreads = (int) (obfuscationWorkerThreadsSet ? obfuscationWorkerThreads : availableThreads);
		this.proximityHiderThreads = (int) (proximityHiderThreadsSet ? proximityHiderThreads : Math.ceil(availableThreads / 2f));

		OFCLogger.setVerboseLogging(this.verbose);
		OFCLogger.debug("advanced.protocolLibThreads = " + this.protocolLibThreads);
		OFCLogger.debug("advanced.obfuscationWorkerThreads = " + this.obfuscationWorkerThreads);
		OFCLogger.debug("advanced.proximityHiderThreads = " + this.proximityHiderThreads);
	}

	public void serialize(ConfigurationSection section) {
		section.set("verbose", this.verbose);
		section.set("useAsyncPacketListener", this.useAsyncPacketListener);
		section.set("maxMillisecondsPerTick", this.maxMillisecondsPerTick);
		section.set("protocolLibThreads", this.protocolLibThreadsSet ? this.protocolLibThreads : -1);

		section.set("obfuscationWorkerThreads", this.obfuscationWorkerThreadsSet ? this.obfuscationWorkerThreads : -1);
		section.set("obfuscationTimeout", this.hasObfuscationTimeout ? this.obfuscationTimeout : -1);

		section.set("proximityHiderThreads", this.proximityHiderThreadsSet ? this.proximityHiderThreads : -1);
		section.set("proximityDefaultBucketSize", this.proximityDefaultBucketSize);
		section.set("proximityThreadCheckInterval", this.proximityThreadCheckInterval);
		section.set("proximityPlayerCheckInterval", this.hasProximityPlayerCheckInterval ? this.proximityPlayerCheckInterval : -1);
	}

	@Override
	public boolean useAsyncPacketListener() {
		return this.useAsyncPacketListener;
	}

	@Override
	public int maxMillisecondsPerTick() {
		return this.maxMillisecondsPerTick;
	}

	@Override
	public int protocolLibThreads() {
		return this.protocolLibThreads;
	}

	@Override
	public int obfuscationWorkerThreads() {
		return this.obfuscationWorkerThreads;
	}

	@Override
	public boolean hasObfuscationTimeout() {
		return this.hasObfuscationTimeout;
	}

	@Override
	public int obfuscationTimeout() {
		return this.obfuscationTimeout;
	}

	@Override
	public int proximityHiderThreads() {
		return this.proximityHiderThreads;
	}

	@Override
	public int proximityDefaultBucketSize() {
		return this.proximityDefaultBucketSize;
	}

	@Override
	public int proximityThreadCheckInterval() {
		return this.proximityThreadCheckInterval;
	}

	@Override
	public boolean hasProximityPlayerCheckInterval() {
		return this.hasProximityPlayerCheckInterval;
	}

	@Override
	public int proximityPlayerCheckInterval() {
		return this.proximityPlayerCheckInterval;
	}
}
