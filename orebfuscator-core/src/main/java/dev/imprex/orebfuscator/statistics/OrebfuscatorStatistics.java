package dev.imprex.orebfuscator.statistics;

import dev.imprex.orebfuscator.config.api.Config;

public class OrebfuscatorStatistics {

  public final CacheStatistics cache;
  public final InjectorStatistics injector = new InjectorStatistics();
  public final ObfuscationStatistics obfuscation = new ObfuscationStatistics();

  public OrebfuscatorStatistics(Config config, StatisticsRegistry registry) {
    this.cache = new CacheStatistics(config);

    if (config.cache().enabled()) {
      registry.register("cache", this.cache);
    }

    registry.register("injector", this.injector);
    registry.register("obfuscation", this.obfuscation);
  }
}
