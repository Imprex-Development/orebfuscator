package net.imprex.orebfuscator.config;

import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.config.context.ConfigParsingContext;
import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorAdvancedConfig implements AdvancedConfig {

  private boolean verbose = false;

  private int obfuscationThreads = -1;
  private long obfuscationTimeout = 10_000;
  private int maxMillisecondsPerTick = 10;

  private int proximityThreads = -1;
  private int proximityDefaultBucketSize = 50;
  private int proximityThreadCheckInterval = 50;
  private int proximityPlayerCheckInterval = 5000;

  private boolean obfuscationWorkerThreadsSet = false;
  private boolean hasObfuscationTimeout = false;
  private boolean proximityHiderThreadsSet = false;
  private boolean hasProximityPlayerCheckInterval = true;

  public void deserialize(ConfigurationSection section, ConfigParsingContext context) {
    this.verbose = section.getBoolean("verbose", false);

    // parse obfuscation section
    ConfigParsingContext obfuscationContext = context.section("obfuscation");
    ConfigurationSection obfuscationSection = section.getConfigurationSection("obfuscation");
    if (obfuscationSection != null) {
      this.obfuscationThreads = obfuscationSection.getInt("threads", -1);
      this.obfuscationWorkerThreadsSet = (this.obfuscationThreads > 0);

      this.obfuscationTimeout = obfuscationSection.getLong("timeout", -1);
      this.hasObfuscationTimeout = (this.obfuscationTimeout > 0);

      this.maxMillisecondsPerTick = obfuscationSection.getInt("maxMillisecondsPerTick", 10);
      obfuscationContext.errorMinMaxValue("maxMillisecondsPerTick", 1, 50, this.maxMillisecondsPerTick);
    } else {
      obfuscationContext.warnMissingSection();
    }

    // parse proximity section
    ConfigParsingContext proximityContext = context.section("proximity");
    ConfigurationSection proximitySection = section.getConfigurationSection("proximity");
    if (proximitySection != null) {
      this.proximityThreads = proximitySection.getInt("threads", -1);
      this.proximityHiderThreadsSet = (this.proximityThreads > 0);

      this.proximityDefaultBucketSize = proximitySection.getInt("defaultBucketSize", 50);
      proximityContext.errorMinValue("defaultBucketSize", 1, this.proximityDefaultBucketSize);

      this.proximityThreadCheckInterval = proximitySection.getInt("threadCheckInterval", 50);
      proximityContext.errorMinValue("threadCheckInterval", 1, this.proximityThreadCheckInterval);

      this.proximityPlayerCheckInterval = proximitySection.getInt("playerCheckInterval", 5000);
      this.hasProximityPlayerCheckInterval = (this.proximityPlayerCheckInterval > 0);
    } else {
      proximityContext.warnMissingSection();
    }
  }

  public void initialize() {
    int availableThreads = Runtime.getRuntime().availableProcessors();
    this.obfuscationThreads = (int) (obfuscationWorkerThreadsSet ? obfuscationThreads : availableThreads);
    this.proximityThreads = (int) (proximityHiderThreadsSet ? proximityThreads : Math.ceil(availableThreads / 2f));

    OFCLogger.setVerboseLogging(this.verbose);
    OFCLogger.debug("advanced.obfuscationWorkerThreads = " + this.obfuscationThreads);
    OFCLogger.debug("advanced.proximityHiderThreads = " + this.proximityThreads);
  }

  public void serialize(ConfigurationSection section) {
    section.set("verbose", this.verbose);

    section.set("obfuscation.threads", this.obfuscationWorkerThreadsSet ? this.obfuscationThreads : -1);
    section.set("obfuscation.timeout", this.hasObfuscationTimeout ? this.obfuscationTimeout : -1);
    section.set("obfuscation.maxMillisecondsPerTick", this.maxMillisecondsPerTick);

    section.set("proximity.threads", this.proximityHiderThreadsSet ? this.proximityThreads : -1);
    section.set("proximity.defaultBucketSize", this.proximityDefaultBucketSize);
    section.set("proximity.threadCheckInterval", this.proximityThreadCheckInterval);
    section.set("proximity.playerCheckInterval",
        this.hasProximityPlayerCheckInterval ? this.proximityPlayerCheckInterval : -1);
  }

  @Override
  public int obfuscationThreads() {
    return this.obfuscationThreads;
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
  public int maxMillisecondsPerTick() {
    return this.maxMillisecondsPerTick;
  }

  @Override
  public int proximityThreads() {
    return this.proximityThreads;
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
