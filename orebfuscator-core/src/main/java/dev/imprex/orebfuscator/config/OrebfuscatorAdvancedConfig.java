package dev.imprex.orebfuscator.config;

import dev.imprex.orebfuscator.config.api.AdvancedConfig;
import dev.imprex.orebfuscator.config.context.ConfigMessage;
import dev.imprex.orebfuscator.config.context.ConfigParsingContext;
import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.logging.OfcLogger;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class OrebfuscatorAdvancedConfig implements AdvancedConfig {

  private boolean verbose = false;
  private int threads = -1;

  private long obfuscationTimeout = 10_000L;

  private int proximityDefaultBucketSize = 50;
  private int proximityThreadCheckInterval = 50;
  private int proximityPlayerCheckInterval = 5000;

  private boolean hasThreads = false;
  private boolean hasObfuscationTimeout = false;
  private boolean hasProximityPlayerCheckInterval = true;

  public void deserialize(ConfigurationSection section, ConfigParsingContext context) {
    this.verbose = section.getBoolean("verbose", false);

    this.threads = section.getInt("threads", -1);
    this.hasThreads = (this.threads > 0);

    // parse obfuscation section
    ConfigParsingContext obfuscationContext = context.section("obfuscation");
    ConfigurationSection obfuscationSection = section.getSection("obfuscation");
    if (obfuscationSection != null) {
      this.obfuscationTimeout = obfuscationSection.getLong("timeout", 10_000L);
      this.hasObfuscationTimeout = (this.obfuscationTimeout > 0);
    } else {
      obfuscationContext.warn(ConfigMessage.MISSING_USING_DEFAULTS);
    }

    // parse proximity section
    ConfigParsingContext proximityContext = context.section("proximity");
    ConfigurationSection proximitySection = section.getSection("proximity");
    if (proximitySection != null) {
      this.proximityDefaultBucketSize = proximitySection.getInt("defaultBucketSize", 50);
      proximityContext.errorMinValue("defaultBucketSize", 1, this.proximityDefaultBucketSize);

      this.proximityThreadCheckInterval = proximitySection.getInt("threadCheckInterval", 50);
      proximityContext.errorMinValue("threadCheckInterval", 1, this.proximityThreadCheckInterval);

      this.proximityPlayerCheckInterval = proximitySection.getInt("playerCheckInterval", 5000);
      this.hasProximityPlayerCheckInterval = (this.proximityPlayerCheckInterval > 0);
    } else {
      proximityContext.warn(ConfigMessage.MISSING_USING_DEFAULTS);
    }

    int availableThreads = Runtime.getRuntime().availableProcessors();
    this.threads = hasThreads ? threads : availableThreads;

    OfcLogger.setVerboseLogging(this.verbose);
    OfcLogger.debug("advanced.threads = " + this.threads);
  }

  public void serialize(ConfigurationSection section) {
    section.set("verbose", this.verbose);
    section.set("threads", this.hasThreads ? this.threads : -1);

    section.set("obfuscation.timeout", this.hasObfuscationTimeout ? this.obfuscationTimeout : -1);

    section.set("proximity.defaultBucketSize", this.proximityDefaultBucketSize);
    section.set("proximity.threadCheckInterval", this.proximityThreadCheckInterval);
    section.set("proximity.playerCheckInterval",
        this.hasProximityPlayerCheckInterval ? this.proximityPlayerCheckInterval : -1);
  }

  @Override
  public int threads() {
    return this.threads;
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
