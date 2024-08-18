package net.imprex.orebfuscator.config;

import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.config.context.ConfigParsingContext;
import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorAdvancedConfig implements AdvancedConfig {

	private boolean verbose = false;
	private int maxMillisecondsPerTick = 10;

	private int obfuscationWorkerThreads = -1;
	private long obfuscationTimeout = 10_000;

	private int proximityHiderThreads = -1;
	private int proximityDefaultBucketSize = 50;
	private int proximityThreadCheckInterval = 50;
	private int proximityPlayerCheckInterval = 5000;

	private boolean obfuscationWorkerThreadsSet = false;
	private boolean hasObfuscationTimeout = false;
	private boolean proximityHiderThreadsSet = false;
	private boolean hasProximityPlayerCheckInterval = true;

	public void deserialize(ConfigurationSection section, ConfigParsingContext context) {
		this.verbose = section.getBoolean("verbose", false);
		this.maxMillisecondsPerTick = section.getInt("maxMillisecondsPerTick", 10);

		if (this.maxMillisecondsPerTick <= 0 || this.maxMillisecondsPerTick >= 50) {
			throw new RuntimeException(
					"maxMillisecondsPerTick has to be between 0 and 50, value: " + this.maxMillisecondsPerTick);
		}

		this.obfuscationWorkerThreads = section.getInt("obfuscationWorkerThreads", -1);
		this.obfuscationWorkerThreadsSet = (this.obfuscationWorkerThreads > 0);

		this.obfuscationTimeout = section.getLong("obfuscationTimeout", -1);
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
		this.obfuscationWorkerThreads = (int) (obfuscationWorkerThreadsSet ? obfuscationWorkerThreads : availableThreads);
		this.proximityHiderThreads = (int) (proximityHiderThreadsSet ? proximityHiderThreads : Math.ceil(availableThreads / 2f));

		OFCLogger.setVerboseLogging(this.verbose);
		OFCLogger.debug("advanced.obfuscationWorkerThreads = " + this.obfuscationWorkerThreads);
		OFCLogger.debug("advanced.proximityHiderThreads = " + this.proximityHiderThreads);
	}

	public void serialize(ConfigurationSection section) {
		section.set("verbose", this.verbose);
		section.set("maxMillisecondsPerTick", this.maxMillisecondsPerTick);

		section.set("obfuscationWorkerThreads", this.obfuscationWorkerThreadsSet ? this.obfuscationWorkerThreads : -1);
		section.set("obfuscationTimeout", this.hasObfuscationTimeout ? this.obfuscationTimeout : -1);

		section.set("proximityHiderThreads", this.proximityHiderThreadsSet ? this.proximityHiderThreads : -1);
		section.set("proximityDefaultBucketSize", this.proximityDefaultBucketSize);
		section.set("proximityThreadCheckInterval", this.proximityThreadCheckInterval);
		section.set("proximityPlayerCheckInterval", this.hasProximityPlayerCheckInterval ? this.proximityPlayerCheckInterval : -1);
	}

	@Override
	public int maxMillisecondsPerTick() {
		return this.maxMillisecondsPerTick;
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
	public long obfuscationTimeout() {
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
