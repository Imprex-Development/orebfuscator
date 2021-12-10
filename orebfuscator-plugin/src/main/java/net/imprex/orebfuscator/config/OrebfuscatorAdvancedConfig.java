package net.imprex.orebfuscator.config;

import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorAdvancedConfig implements AdvancedConfig {

	private boolean verbose = false;
	private int maxMillisecondPerTick = 10;
	private int protocolLibThreads = -1;
	private int obfuscationWorkerThreads = -1;

	private boolean protocolLibThreadsSet = false;
	private boolean obfuscationWorkerThreadsSet = false;

	public void deserialize(ConfigurationSection section) {
		this.verbose = section.getBoolean("verbose", false);
		this.maxMillisecondPerTick = section.getInt("maxMillisecondPerTick", 10);

		this.protocolLibThreads = section.getInt("protocolLibThreads", -1);
		this.protocolLibThreadsSet = (this.protocolLibThreads > 0);

		this.obfuscationWorkerThreads = section.getInt("obfuscationWorkerThreads", -1);
		this.obfuscationWorkerThreadsSet = (this.obfuscationWorkerThreads > 0);
	}

	public void initialize() {
		int availableThreads = Runtime.getRuntime().availableProcessors();
		this.protocolLibThreads = (int) (protocolLibThreadsSet ? protocolLibThreads : Math.ceil(availableThreads / 2f));
		this.obfuscationWorkerThreads = (int) (obfuscationWorkerThreadsSet ? obfuscationWorkerThreads : availableThreads);

		OFCLogger.setVerboseLogging(this.verbose);
		OFCLogger.debug("advanced.maxMillisecondPerTick = " + this.maxMillisecondPerTick);
		OFCLogger.debug("advanced.protocolLibThreads = " + this.protocolLibThreads);
		OFCLogger.debug("advanced.obfuscationWorkerThreads = " + this.obfuscationWorkerThreads);
	}

	public void serialize(ConfigurationSection section) {
		section.set("verbose", this.verbose);
		section.set("maxMillisecondPerTick", this.maxMillisecondPerTick);
		section.set("protocolLibThreads", this.protocolLibThreadsSet ? this.protocolLibThreads : -1);
		section.set("obfuscationWorkerThreads", this.obfuscationWorkerThreadsSet ? this.obfuscationWorkerThreads : -1);
	}

	@Override
	public int maxMillisecondPerTick() {
		return this.maxMillisecondPerTick;
	}

	@Override
	public int protocolLibThreads() {
		return this.protocolLibThreads;
	}

	@Override
	public int obfuscationWorkerThreads() {
		return this.obfuscationWorkerThreads;
	}
}
